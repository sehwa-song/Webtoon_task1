package variants.factory;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/** Thread-safe, bounded, access-order LRU repository owned by the cached family. */
final class SingletonLruImageRepository implements ImageRepository {
    private static final int DEFAULT_CAPACITY = 4;
    private static final String CAPACITY_PROPERTY =
            "variants.factory.cache.maxEntries";

    private final int capacity;
    private final LinkedHashMap<CacheKey, BufferedImage> cache;
    private long hitCount;
    private long missCount;
    private long evictionCount;

    private SingletonLruImageRepository() {
        capacity = Math.max(1, Integer.getInteger(
                CAPACITY_PROPERTY, DEFAULT_CAPACITY));
        cache = new LinkedHashMap<CacheKey, BufferedImage>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(
                    Map.Entry<CacheKey, BufferedImage> eldest) {
                boolean remove = size() > SingletonLruImageRepository.this.capacity;
                if (remove) {
                    evictionCount++;
                }
                return remove;
            }
        };
    }

    private static final class Holder {
        private static final SingletonLruImageRepository INSTANCE =
                new SingletonLruImageRepository();
    }

    static SingletonLruImageRepository getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public synchronized BufferedImage load(Path source) throws IOException {
        CacheKey key = CacheKey.from(source);
        BufferedImage existing = cache.get(key);
        if (existing != null) {
            hitCount++;
            return existing;
        }

        BufferedImage loaded = ImageIO.read(key.path.toFile());
        if (loaded == null) {
            throw new IOException("Unsupported or damaged image: " + key.path);
        }

        // A path whose metadata changed replaces, rather than coexists with,
        // its stale cache entry.
        Iterator<CacheKey> keys = cache.keySet().iterator();
        while (keys.hasNext()) {
            if (keys.next().path.equals(key.path)) {
                keys.remove();
            }
        }

        cache.put(key, loaded);
        missCount++;
        return loaded;
    }

    synchronized void clear() {
        cache.clear();
        hitCount = 0L;
        missCount = 0L;
        evictionCount = 0L;
    }

    synchronized CacheState snapshot() {
        long bytes = 0L;
        for (BufferedImage image : cache.values()) {
            DataBuffer buffer = image.getRaster().getDataBuffer();
            int bytesPerElement = Math.max(
                    1, DataBuffer.getDataTypeSize(buffer.getDataType()) / 8);
            bytes += (long) buffer.getSize()
                    * buffer.getNumBanks()
                    * bytesPerElement;
        }
        return new CacheState(
                cache.size(), capacity, hitCount, missCount, evictionCount, bytes);
    }

    static final class CacheState {
        final int size;
        final int capacity;
        final long hits;
        final long misses;
        final long evictions;
        final long estimatedPixelBytes;

        CacheState(
                int size,
                int capacity,
                long hits,
                long misses,
                long evictions,
                long estimatedPixelBytes) {
            this.size = size;
            this.capacity = capacity;
            this.hits = hits;
            this.misses = misses;
            this.evictions = evictions;
            this.estimatedPixelBytes = estimatedPixelBytes;
        }
    }

    private static final class CacheKey {
        final Path path;
        final long size;
        final long modifiedMillis;

        CacheKey(Path path, long size, long modifiedMillis) {
            this.path = path;
            this.size = size;
            this.modifiedMillis = modifiedMillis;
        }

        static CacheKey from(Path source) throws IOException {
            Path normalized = source.toAbsolutePath().normalize();
            return new CacheKey(
                    normalized,
                    Files.size(normalized),
                    Files.getLastModifiedTime(normalized).toMillis());
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof CacheKey)) {
                return false;
            }
            CacheKey that = (CacheKey) other;
            return size == that.size
                    && modifiedMillis == that.modifiedMillis
                    && path.equals(that.path);
        }

        @Override
        public int hashCode() {
            int result = path.hashCode();
            result = 31 * result + Long.hashCode(size);
            result = 31 * result + Long.hashCode(modifiedMillis);
            return result;
        }
    }
}

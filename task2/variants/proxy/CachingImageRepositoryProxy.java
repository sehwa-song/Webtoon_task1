package task2.variants.proxy;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Proxy that adds a bounded, access-ordered LRU cache in front of another
 * {@link ImageRepository}. Every instance owns its cache; this class is
 * intentionally not a Singleton.
 */
public final class CachingImageRepositoryProxy implements ImageRepository {
    public static final int DEFAULT_CAPACITY = 4;

    private final ImageRepository realSubject;
    private final int capacity;
    private final LinkedHashMap<CacheKey, BufferedImage> cache;
    private long hitCount;
    private long missCount;
    private long evictionCount;

    public CachingImageRepositoryProxy(ImageRepository realSubject) {
        this(realSubject, DEFAULT_CAPACITY);
    }

    public CachingImageRepositoryProxy(
            ImageRepository realSubject, int capacity) {
        this.realSubject = Objects.requireNonNull(realSubject, "realSubject");
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.cache = new LinkedHashMap<CacheKey, BufferedImage>(
                Math.max(2, capacity + 1), 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(
                    Map.Entry<CacheKey, BufferedImage> eldest) {
                boolean remove = size()
                        > CachingImageRepositoryProxy.this.capacity;
                if (remove) {
                    evictionCount++;
                }
                return remove;
            }
        };
    }

    /**
     * Returns a cached image when the path, file size, and modification time
     * still match; otherwise delegates to the RealSubject.
     */
    @Override
    public synchronized BufferedImage load(Path source) throws IOException {
        CacheKey key = CacheKey.from(source);
        BufferedImage cached = cache.get(key);
        if (cached != null) {
            hitCount++;
            return cached;
        }

        BufferedImage loaded = realSubject.load(key.path);

        // A changed file has a new key. Remove its stale version before the
        // new value is inserted so one path never occupies multiple slots.
        Iterator<CacheKey> iterator = cache.keySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().path.equals(key.path)) {
                iterator.remove();
            }
        }
        cache.put(key, loaded);
        missCount++;
        return loaded;
    }

    /** Clears this proxy instance without affecting any other instance. */
    public synchronized void clear() {
        cache.clear();
        hitCount = 0L;
        missCount = 0L;
        evictionCount = 0L;
    }

    public synchronized CacheStats stats() {
        long estimatedPixelBytes = 0L;
        for (BufferedImage image : cache.values()) {
            DataBuffer buffer = image.getRaster().getDataBuffer();
            int bytesPerElement = Math.max(
                    1, DataBuffer.getDataTypeSize(buffer.getDataType()) / 8);
            estimatedPixelBytes += (long) buffer.getSize()
                    * buffer.getNumBanks()
                    * bytesPerElement;
        }
        return new CacheStats(
                cache.size(),
                capacity,
                hitCount,
                missCount,
                evictionCount,
                estimatedPixelBytes);
    }

    public int capacity() {
        return capacity;
    }

    public ImageRepository realSubject() {
        return realSubject;
    }

    /** Immutable observation of one proxy instance's cache state. */
    public static final class CacheStats {
        private final int size;
        private final int capacity;
        private final long hits;
        private final long misses;
        private final long evictions;
        private final long estimatedPixelBytes;

        private CacheStats(
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

        public int size() {
            return size;
        }

        public int capacity() {
            return capacity;
        }

        public long hits() {
            return hits;
        }

        public long misses() {
            return misses;
        }

        public long evictions() {
            return evictions;
        }

        public long estimatedPixelBytes() {
            return estimatedPixelBytes;
        }

        @Override
        public String toString() {
            return "cache{size=" + size
                    + "/" + capacity
                    + ", hits=" + hits
                    + ", misses=" + misses
                    + ", evictions=" + evictions
                    + ", estimatedPixelBytes=" + estimatedPixelBytes
                    + "}";
        }
    }

    private static final class CacheKey {
        private final Path path;
        private final long size;
        private final long modifiedMillis;

        private CacheKey(Path path, long size, long modifiedMillis) {
            this.path = path;
            this.size = size;
            this.modifiedMillis = modifiedMillis;
        }

        private static CacheKey from(Path source) throws IOException {
            Path normalized = Objects.requireNonNull(source, "source")
                    .toAbsolutePath().normalize();
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
            result = 31 * result + Long.valueOf(size).hashCode();
            result = 31 * result + Long.valueOf(modifiedMillis).hashCode();
            return result;
        }
    }
}

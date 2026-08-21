import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * 여러 경로의 BufferedImage를 재사용하는 Singleton LRU 저장소.
 * 무제한 Map은 긴 웹툰에서 OOM을 일으킬 수 있으므로 항목 수를 제한한다.
 */
final class SingletonImageRepository implements ImageRepository {
    private static final int DEFAULT_MAX_ENTRIES = 4;

    private final int maxEntries;
    private final Map<ImageCacheKey, BufferedImage> images;
    private long hitCount;
    private long missCount;

    private SingletonImageRepository() {
        maxEntries = Math.max(1,
                Integer.getInteger("webtoon.cache.maxEntries", DEFAULT_MAX_ENTRIES));
        images = new LinkedHashMap<ImageCacheKey, BufferedImage>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(
                    Map.Entry<ImageCacheKey, BufferedImage> eldest) {
                return size() > SingletonImageRepository.this.maxEntries;
            }
        };
    }

    private static final class Holder {
        private static final SingletonImageRepository INSTANCE =
                new SingletonImageRepository();
    }

    static SingletonImageRepository getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public synchronized BufferedImage load(Path source) throws IOException {
        ImageCacheKey key = ImageCacheKey.from(source);
        BufferedImage cached = images.get(key);
        if (cached != null) {
            hitCount++;
            return cached;
        }

        BufferedImage loaded = ImageIO.read(key.path.toFile());
        if (loaded == null) {
            throw new IOException("지원하지 않는 이미지입니다: " + key.path);
        }

        // 같은 경로의 파일이 수정된 경우 이전 메타데이터 키를 남기지 않는다.
        images.keySet().removeIf(existing -> existing.path.equals(key.path));
        images.put(key, loaded);
        missCount++;
        return loaded;
    }

    synchronized void clear() {
        images.clear();
        hitCount = 0;
        missCount = 0;
    }

    synchronized int size() {
        return images.size();
    }

    synchronized int capacity() {
        return maxEntries;
    }

    synchronized long hitCount() {
        return hitCount;
    }

    synchronized long missCount() {
        return missCount;
    }

    /** 래스터 픽셀 배열만 계산한 근삿값이며 객체 헤더 등은 포함하지 않는다. */
    synchronized long estimatedPixelBytes() {
        long bytes = 0;
        for (BufferedImage image : images.values()) {
            DataBuffer buffer = image.getRaster().getDataBuffer();
            int bytesPerElement = DataBuffer.getDataTypeSize(buffer.getDataType()) / 8;
            bytes += (long) buffer.getSize() * buffer.getNumBanks() * bytesPerElement;
        }
        return bytes;
    }

    /** 파일 교체 후 낡은 이미지를 반환하지 않도록 경로, 크기, 수정 시각을 함께 비교한다. */
    private static final class ImageCacheKey {
        private final Path path;
        private final long size;
        private final long lastModifiedMillis;

        private ImageCacheKey(Path path, long size, long lastModifiedMillis) {
            this.path = path;
            this.size = size;
            this.lastModifiedMillis = lastModifiedMillis;
        }

        static ImageCacheKey from(Path source) throws IOException {
            Path normalized = source.toAbsolutePath().normalize();
            return new ImageCacheKey(
                    normalized,
                    Files.size(normalized),
                    Files.getLastModifiedTime(normalized).toMillis());
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ImageCacheKey)) {
                return false;
            }
            ImageCacheKey that = (ImageCacheKey) other;
            return size == that.size
                    && lastModifiedMillis == that.lastModifiedMillis
                    && path.equals(that.path);
        }

        @Override
        public int hashCode() {
            int result = path.hashCode();
            result = 31 * result + Long.hashCode(size);
            result = 31 * result + Long.hashCode(lastModifiedMillis);
            return result;
        }
    }
}

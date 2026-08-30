package task1.proxy;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * ImageRepository 호출을 대신 받고, 이미 읽은 경로이면 메모리 캐시에서 반환하는 Proxy입니다.
 */
public final class CachingImageRepositoryProxy implements ImageRepository {
    private final ImageRepository realSubject;
    private final Map<String, BufferedImage> cache = new HashMap<>();
    private long hitCount;
    private long missCount;

    public CachingImageRepositoryProxy(ImageRepository realSubject) {
        this.realSubject = Objects.requireNonNull(realSubject, "realSubject");
    }

    @Override
    public BufferedImage read(File imageFile) throws IOException {
        String key = imageFile.getCanonicalPath();
        BufferedImage cached = cache.get(key);
        if (cached != null) {
            hitCount++;
            return cached;
        }

        missCount++;
        BufferedImage loaded = realSubject.read(imageFile);
        cache.put(key, loaded);
        return loaded;
    }

    public long getHitCount() {
        return hitCount;
    }

    public long getMissCount() {
        return missCount;
    }

    public int getCachedImageCount() {
        return cache.size();
    }

    public void clear() {
        cache.clear();
    }
}

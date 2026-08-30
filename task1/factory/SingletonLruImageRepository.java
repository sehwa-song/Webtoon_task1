package task1.factory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 최근 이미지를 보관하는 캐시 제품입니다. 동일 경로를 다시 읽으면 디스크 I/O를 생략합니다.
 * 저장 개수를 제한한 LRU이므로 무제한 캐시보다 메모리 사용을 통제하기 쉽습니다.
 */
public final class SingletonLruImageRepository implements ImageRepository {
    private static final int CAPACITY = 8;
    private static final SingletonLruImageRepository INSTANCE =
            new SingletonLruImageRepository();

    private final ImageRepository disk = new DiskImageRepository();
    private final Map<String, BufferedImage> cache =
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<String, BufferedImage> eldest) {
                    return size() > CAPACITY;
                }
            };

    private SingletonLruImageRepository() {
    }

    public static SingletonLruImageRepository getInstance() {
        return INSTANCE;
    }

    @Override
    public synchronized BufferedImage read(File file) throws IOException {
        String key = file.getCanonicalPath();
        BufferedImage image = cache.get(key);
        if (image == null) {
            image = disk.read(file);
            cache.put(key, image);
        }
        return image;
    }

    public synchronized void clear() {
        cache.clear();
    }

    @Override
    public String name() {
        return "singleton-lru-image-repository";
    }
}

package task2.variants.factory;

/** Concrete family that shares only the bounded Singleton LRU image cache. */
public final class CachedProcessingFactory implements ProcessingFamilyFactory {
    @Override
    public ImageRepository createImageRepository() {
        return SingletonLruImageRepository.getInstance();
    }

    @Override
    public ContentRegionDetector createContentRegionDetector() {
        return new RgbMarginContentRegionDetector();
    }

    @Override
    public RegionWriter createRegionWriter() {
        return new JpegRegionWriter();
    }

    @Override
    public String name() {
        return "TASK2_ABSTRACT_FACTORY_SINGLETON_LRU";
    }

    public void clearCache() {
        SingletonLruImageRepository.getInstance().clear();
    }

    public CacheSnapshot cacheSnapshot() {
        SingletonLruImageRepository.CacheState state =
                SingletonLruImageRepository.getInstance().snapshot();
        return new CacheSnapshot(
                state.size,
                state.capacity,
                state.hits,
                state.misses,
                state.evictions,
                state.estimatedPixelBytes);
    }

    /** Immutable cache diagnostics for later timing and memory experiments. */
    public static final class CacheSnapshot {
        private final int size;
        private final int capacity;
        private final long hits;
        private final long misses;
        private final long evictions;
        private final long estimatedPixelBytes;

        CacheSnapshot(
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
            return "CacheSnapshot{"
                    + "size=" + size
                    + ", capacity=" + capacity
                    + ", hits=" + hits
                    + ", misses=" + misses
                    + ", evictions=" + evictions
                    + ", estimatedPixelBytes=" + estimatedPixelBytes
                    + '}';
        }
    }
}

package variants.factory;

/**
 * Singleton concrete factory backed by the process-wide bounded LRU image
 * repository.
 */
public final class CachedProcessingFactory implements ProcessingFamilyFactory {
    private CachedProcessingFactory() {
    }

    private static final class Holder {
        private static final CachedProcessingFactory INSTANCE =
                new CachedProcessingFactory();
    }

    public static CachedProcessingFactory getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public ImageRepository createImageRepository() {
        return SingletonLruImageRepository.getInstance();
    }

    @Override
    public CutPointDetector createCutPointDetector() {
        return RgbMarginCutPointDetector.getInstance();
    }

    @Override
    public PieceWriter createPieceWriter() {
        return JpegPieceWriter.getInstance();
    }

    @Override
    public String name() {
        return "ABSTRACT_FACTORY_SINGLETON_LRU";
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

    /** Immutable public cache metrics for benchmarks and diagnostics. */
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

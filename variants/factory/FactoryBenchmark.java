package variants.factory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Public benchmark API for this isolated experiment. */
public final class FactoryBenchmark {
    private FactoryBenchmark() {
    }

    /**
     * Measures a real factory-created service workload. No detector or default
     * package implementation is invoked directly.
     */
    public static Result run(
            ProcessingFamilyFactory factory,
            List<Path> sources,
            int repetitions) throws IOException {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(sources, "sources");
        if (sources.isEmpty()) {
            throw new IllegalArgumentException("sources must not be empty");
        }
        if (repetitions <= 0) {
            throw new IllegalArgumentException("repetitions must be positive");
        }

        WebtoonProcessingService service =
                new WebtoonProcessingService(factory);
        long started = System.nanoTime();
        long checksum = service.benchmark(sources, repetitions);
        long elapsed = System.nanoTime() - started;
        return new Result(
                factory.name(),
                sources.size() * repetitions,
                elapsed,
                checksum);
    }

    public static Result run(
            boolean cached, List<Path> sources, int repetitions)
            throws IOException {
        return run(FactorySplitterApp.createFactory(cached), sources, repetitions);
    }

    /** Runs both families and verifies that their detector results are identical. */
    public static Comparison compare(
            List<Path> sources, int repetitions) throws IOException {
        Result standard = run(
                new StandardProcessingFactory(), sources, repetitions);

        CachedProcessingFactory cachedFactory =
                CachedProcessingFactory.getInstance();
        cachedFactory.clearCache();
        Result cached = run(cachedFactory, sources, repetitions);
        if (standard.checksum() != cached.checksum()) {
            throw new IllegalStateException(
                    "Standard and cached factory results differ");
        }
        return new Comparison(
                standard, cached, cachedFactory.cacheSnapshot());
    }

    public static final class Result {
        private final String factoryName;
        private final int operations;
        private final long elapsedNanos;
        private final long checksum;

        Result(
                String factoryName,
                int operations,
                long elapsedNanos,
                long checksum) {
            this.factoryName = factoryName;
            this.operations = operations;
            this.elapsedNanos = elapsedNanos;
            this.checksum = checksum;
        }

        public String factoryName() {
            return factoryName;
        }

        public int operations() {
            return operations;
        }

        public long elapsedNanos() {
            return elapsedNanos;
        }

        public double millisecondsPerOperation() {
            return elapsedNanos / 1_000_000.0 / operations;
        }

        public long checksum() {
            return checksum;
        }

        @Override
        public String toString() {
            return "Result{"
                    + "factory='" + factoryName + '\''
                    + ", operations=" + operations
                    + ", elapsedNanos=" + elapsedNanos
                    + ", checksum=" + checksum
                    + '}';
        }
    }

    public static final class Comparison {
        private final Result standard;
        private final Result cached;
        private final CachedProcessingFactory.CacheSnapshot cacheSnapshot;

        Comparison(
                Result standard,
                Result cached,
                CachedProcessingFactory.CacheSnapshot cacheSnapshot) {
            this.standard = standard;
            this.cached = cached;
            this.cacheSnapshot = cacheSnapshot;
        }

        public Result standard() {
            return standard;
        }

        public Result cached() {
            return cached;
        }

        public CachedProcessingFactory.CacheSnapshot cacheSnapshot() {
            return cacheSnapshot;
        }
    }
}

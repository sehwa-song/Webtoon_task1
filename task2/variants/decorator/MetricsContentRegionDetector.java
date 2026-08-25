package task2.variants.decorator;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/** Concrete Decorator that records observation metrics without changing coordinates. */
public final class MetricsContentRegionDetector
        extends ContentRegionDetectorDecorator {
    private final AtomicLong calls = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();
    private final AtomicLong totalRegions = new AtomicLong();
    private final AtomicLong totalNanos = new AtomicLong();

    public MetricsContentRegionDetector(ContentRegionDetector delegate) {
        super(delegate);
    }

    @Override
    public List<ContentRegion> findContentRegions(BufferedImage image) {
        long startedAt = System.nanoTime();
        boolean succeeded = false;
        try {
            List<ContentRegion> regions = delegate.findContentRegions(image);
            totalRegions.addAndGet(regions.size());
            succeeded = true;
            return regions;
        } finally {
            totalNanos.addAndGet(System.nanoTime() - startedAt);
            calls.incrementAndGet();
            if (!succeeded) {
                failures.incrementAndGet();
            }
        }
    }

    @Override
    public String name() {
        return "metrics(" + delegate.name() + ")";
    }

    public MetricsSnapshot snapshot() {
        return new MetricsSnapshot(
                calls.get(),
                failures.get(),
                totalRegions.get(),
                totalNanos.get());
    }

    /** Immutable observation captured from this Decorator. */
    public static final class MetricsSnapshot {
        private final long calls;
        private final long failures;
        private final long totalRegions;
        private final long totalNanos;

        MetricsSnapshot(
                long calls,
                long failures,
                long totalRegions,
                long totalNanos) {
            this.calls = calls;
            this.failures = failures;
            this.totalRegions = totalRegions;
            this.totalNanos = totalNanos;
        }

        public long calls() {
            return calls;
        }

        public long failures() {
            return failures;
        }

        public long totalRegions() {
            return totalRegions;
        }

        public long totalNanos() {
            return totalNanos;
        }

        public double averageMillis() {
            return calls == 0L ? 0.0 : totalNanos / 1_000_000.0 / calls;
        }

        @Override
        public String toString() {
            return String.format(
                    Locale.ROOT,
                    "MetricsSnapshot{calls=%d, failures=%d, regions=%d, "
                            + "totalMs=%.3f, averageMs=%.3f}",
                    calls,
                    failures,
                    totalRegions,
                    totalNanos / 1_000_000.0,
                    averageMillis());
        }
    }
}

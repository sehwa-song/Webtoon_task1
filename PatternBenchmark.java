import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

/**
 * Lightweight, dependency-free benchmark for the isolated design-pattern
 * implementations.
 *
 * <p>This is intentionally not a replacement for JMH. It performs warm-up,
 * consumes every result, and reports current-thread allocations when the JDK
 * exposes that counter. Run it in a quiet process and compare several runs.</p>
 */
public final class PatternBenchmark {
    private static final int DEFAULT_ITERATIONS = 3;
    private static final int DEFAULT_WARMUP = 2;
    private static final int DEFAULT_LIMIT = 3;
    private static volatile long blackhole;

    private PatternBenchmark() {
    }

    public static void main(String[] args) throws Exception {
        BenchmarkOptions options = BenchmarkOptions.parse(args);
        if (options.help) {
            printUsage();
            return;
        }

        List<Path> paths = collectImages(options.input, options.limit);
        if (paths.isEmpty()) {
            throw new IllegalArgumentException(
                    "No supported image was found: " + options.input);
        }

        List<LoadedImage> loadedImages = loadImages(paths);
        System.out.printf(Locale.ROOT,
                "Input=%s, images=%d, warmup=%d, iterations=%d%n",
                options.input.toAbsolutePath().normalize(),
                paths.size(),
                options.warmup,
                options.iterations);
        System.out.printf(Locale.ROOT,
                "Java=%s, OS=%s, processors=%d%n",
                System.getProperty("java.version"),
                System.getProperty("os.name"),
                Runtime.getRuntime().availableProcessors());

        verifyAllCoordinates(loadedImages);
        runPreloadedAlgorithmBenchmark(loadedImages, options);
        runFactoryLoadingBenchmark(paths, options);

        System.out.println();
        System.out.println("Notes:");
        System.out.println("- Preloaded measurements exclude ImageIO decoding and JPEG output.");
        System.out.println("- Factory STANDARD includes decoding on every operation.");
        System.out.println("- Factory CACHED_COLD starts empty; CACHED_WARM is prefilled.");
        System.out.println("- The OS file cache, JIT and GC can affect results; compare repeated JVM runs.");
    }

    private static void verifyAllCoordinates(List<LoadedImage> loadedImages)
            throws IOException {
        Map<Path, List<Integer>> expected = new LinkedHashMap<>();
        Map<Path, BufferedImage> visitorInput = new LinkedHashMap<>();

        for (LoadedImage loaded : loadedImages) {
            expected.put(loaded.path, Task1Splitter.findCutPoints(loaded.image));
            visitorInput.put(loaded.path, loaded.image);
        }

        Map<Path, List<Integer>> visitorResults =
                variants.visitor.VisitorSplitterApp.analyzeLoadedImages(visitorInput);

        variants.factory.ProcessingFamilyFactory standardFactory =
                variants.factory.FactorySplitterApp.createFactory(false);
        variants.factory.CachedProcessingFactory cachedFactory =
                (variants.factory.CachedProcessingFactory)
                        variants.factory.FactorySplitterApp.createFactory(true);
        variants.factory.CutPointDetector factoryDetector =
                standardFactory.createCutPointDetector();
        variants.factory.WebtoonProcessingService standardService =
                new variants.factory.WebtoonProcessingService(standardFactory);
        variants.factory.WebtoonProcessingService cachedService =
                new variants.factory.WebtoonProcessingService(cachedFactory);

        cachedFactory.clearCache();
        for (LoadedImage loaded : loadedImages) {
            List<Integer> baseline = expected.get(loaded.path);
            assertSameCuts(
                    loaded.path,
                    "STRATEGY",
                    baseline,
                    variants.strategy.StrategySplitterApp.findCutPoints(loaded.image));
            assertSameCuts(
                    loaded.path,
                    "VISITOR",
                    baseline,
                    visitorResults.get(loaded.path));
            assertSameCuts(
                    loaded.path,
                    "FACTORY_PRELOADED_DETECTOR",
                    baseline,
                    factoryDetector.findCutPoints(loaded.image));
            assertSameCuts(
                    loaded.path,
                    "FACTORY_STANDARD",
                    baseline,
                    standardService.detect(loaded.path));
            assertSameCuts(
                    loaded.path,
                    "FACTORY_CACHED",
                    baseline,
                    cachedService.detect(loaded.path));
        }
        cachedFactory.clearCache();

        System.out.printf(Locale.ROOT,
                "Coordinate verification: PASS (%d images, baseline = strategy = visitor "
                        + "= factory standard = factory cached)%n",
                loadedImages.size());
    }

    private static void assertSameCuts(
            Path path,
            String implementation,
            List<Integer> expected,
            List<Integer> actual) {
        if (actual == null || !expected.equals(actual)) {
            throw new IllegalStateException(
                    "Cut coordinates differ for " + implementation + " at " + path
                            + System.lineSeparator() + "expected=" + expected
                            + System.lineSeparator() + "actual=" + actual);
        }
    }

    private static void runPreloadedAlgorithmBenchmark(
            List<LoadedImage> loadedImages,
            BenchmarkOptions options) throws Exception {
        List<BufferedImage> mutableImages = new ArrayList<>();
        for (LoadedImage loaded : loadedImages) {
            mutableImages.add(loaded.image);
        }
        final List<BufferedImage> images =
                Collections.unmodifiableList(mutableImages);

        variants.factory.CutPointDetector factoryDetector =
                variants.factory.FactorySplitterApp.createFactory(false)
                        .createCutPointDetector();

        for (int i = 0; i < options.warmup; i++) {
            consume(runBaseline(images, 1));
            consume(runStrategy(images, 1));
            consume(variants.visitor.VisitorSplitterApp.benchmarkLoadedImages(images, 1));
            consume(runFactoryDetector(images, factoryDetector, 1));
        }

        long operations = Math.multiplyExact(
                (long) images.size(), (long) options.iterations);
        List<Measurement> measurements = new ArrayList<>();
        measurements.add(measure(
                "BASELINE_TASK1SPLITTER",
                operations,
                () -> runBaseline(images, options.iterations)));
        measurements.add(measure(
                "STRATEGY_PUBLIC_API",
                operations,
                () -> runStrategy(images, options.iterations)));
        measurements.add(measure(
                "VISITOR_PUBLIC_API",
                operations,
                () -> variants.visitor.VisitorSplitterApp
                        .benchmarkLoadedImages(images, options.iterations)));
        measurements.add(measure(
                "FACTORY_DETECTOR_PRODUCT",
                operations,
                () -> runFactoryDetector(
                        images, factoryDetector, options.iterations)));

        System.out.println();
        System.out.println("[A] PRELOADED ALGORITHM / PATTERN PATH");
        System.out.println("    Same BufferedImage objects; file loading and output are excluded.");
        printMeasurements(measurements);
    }

    private static long runBaseline(List<BufferedImage> images, int repetitions) {
        long checksum = 1L;
        for (int repetition = 0; repetition < repetitions; repetition++) {
            for (BufferedImage image : images) {
                List<Integer> cuts = Task1Splitter.findCutPoints(image);
                checksum = 31L * checksum + cuts.hashCode();
            }
        }
        return checksum;
    }

    private static long runStrategy(List<BufferedImage> images, int repetitions) {
        long checksum = 1L;
        for (int repetition = 0; repetition < repetitions; repetition++) {
            for (BufferedImage image : images) {
                List<Integer> cuts =
                        variants.strategy.StrategySplitterApp.findCutPoints(image);
                checksum = 31L * checksum + cuts.hashCode();
            }
        }
        return checksum;
    }

    private static long runFactoryDetector(
            List<BufferedImage> images,
            variants.factory.CutPointDetector detector,
            int repetitions) {
        long checksum = 1L;
        for (int repetition = 0; repetition < repetitions; repetition++) {
            for (BufferedImage image : images) {
                List<Integer> cuts = detector.findCutPoints(image);
                checksum = 31L * checksum + cuts.hashCode();
            }
        }
        return checksum;
    }

    private static void runFactoryLoadingBenchmark(
            List<Path> allPaths, BenchmarkOptions options) throws Exception {
        variants.factory.ProcessingFamilyFactory standardFactory =
                variants.factory.FactorySplitterApp.createFactory(false);
        variants.factory.CachedProcessingFactory cachedFactory =
                (variants.factory.CachedProcessingFactory)
                        variants.factory.FactorySplitterApp.createFactory(true);

        cachedFactory.clearCache();
        int capacity = cachedFactory.cacheSnapshot().capacity();
        int workingSetSize = Math.min(allPaths.size(), capacity);
        List<Path> paths = Collections.unmodifiableList(
                new ArrayList<>(allPaths.subList(0, workingSetSize)));

        if (workingSetSize < allPaths.size()) {
            System.out.printf(Locale.ROOT,
                    "Factory warm working set reduced from %d to cache capacity %d. "
                            + "Use -Dvariants.factory.cache.maxEntries=%d to include all.%n",
                    allPaths.size(), capacity, allPaths.size());
        }

        // Warm classes, JIT paths, and ImageIO outside every timed section.
        for (int i = 0; i < options.warmup; i++) {
            consume(variants.factory.FactoryBenchmark
                    .run(standardFactory, paths, 1).checksum());
            cachedFactory.clearCache();
            consume(variants.factory.FactoryBenchmark
                    .run(cachedFactory, paths, 1).checksum());
            consume(variants.factory.FactoryBenchmark
                    .run(cachedFactory, paths, 1).checksum());
        }

        long repeatedOperations = Math.multiplyExact(
                (long) paths.size(), (long) options.iterations);
        Measurement standard = measure(
                "FACTORY_STANDARD_DISK",
                repeatedOperations,
                () -> variants.factory.FactoryBenchmark
                        .run(standardFactory, paths, options.iterations)
                        .checksum());

        // Cold means an empty application cache and exactly one traversal.
        cachedFactory.clearCache();
        Measurement cachedCold = measure(
                "FACTORY_CACHED_COLD",
                paths.size(),
                () -> variants.factory.FactoryBenchmark
                        .run(cachedFactory, paths, 1)
                        .checksum());
        variants.factory.CachedProcessingFactory.CacheSnapshot coldSnapshot =
                cachedFactory.cacheSnapshot();
        if (coldSnapshot.hits() != 0L
                || coldSnapshot.misses() != paths.size()) {
            throw new IllegalStateException(
                    "Cold cache invariant failed: " + coldSnapshot);
        }

        // Fill outside the timer, then measure only cache hits plus detection.
        cachedFactory.clearCache();
        consume(variants.factory.FactoryBenchmark
                .run(cachedFactory, paths, 1).checksum());
        variants.factory.CachedProcessingFactory.CacheSnapshot beforeWarm =
                cachedFactory.cacheSnapshot();
        Measurement cachedWarm = measure(
                "FACTORY_CACHED_WARM",
                repeatedOperations,
                () -> variants.factory.FactoryBenchmark
                        .run(cachedFactory, paths, options.iterations)
                        .checksum());
        variants.factory.CachedProcessingFactory.CacheSnapshot afterWarm =
                cachedFactory.cacheSnapshot();

        long warmHits = afterWarm.hits() - beforeWarm.hits();
        long warmMisses = afterWarm.misses() - beforeWarm.misses();
        if (warmHits != repeatedOperations || warmMisses != 0L) {
            throw new IllegalStateException(
                    "Warm cache invariant failed: hits=" + warmHits
                            + ", misses=" + warmMisses
                            + ", expected hits=" + repeatedOperations);
        }
        if (standard.checksum != cachedWarm.checksum) {
            throw new IllegalStateException(
                    "Factory STANDARD and CACHED_WARM checksums differ");
        }

        System.out.println();
        System.out.println("[B] FACTORY LOAD + DETECT (JPEG output excluded)");
        System.out.printf(Locale.ROOT,
                "    Working set=%d, cache capacity=%d%n",
                paths.size(), capacity);
        printMeasurements(asList(standard, cachedCold, cachedWarm));
        System.out.printf(Locale.ROOT,
                "    COLD cache : hits=%d, misses=%d, evictions=%d%n",
                coldSnapshot.hits(),
                coldSnapshot.misses(),
                coldSnapshot.evictions());
        System.out.printf(Locale.ROOT,
                "    WARM timed : hits=%d, misses=%d, retained entries=%d, "
                        + "estimated pixels=%.2f MiB%n",
                warmHits,
                warmMisses,
                afterWarm.size(),
                bytesToMiB(afterWarm.estimatedPixelBytes()));

        cachedFactory.clearCache();
    }

    private static List<Measurement> asList(
            Measurement first, Measurement second, Measurement third) {
        List<Measurement> result = new ArrayList<>();
        result.add(first);
        result.add(second);
        result.add(third);
        return result;
    }

    private static Measurement measure(
            String name, long operations, CheckedLongTask task) throws Exception {
        forceGcOutsideTimedSection();
        long allocatedBefore = allocatedBytesForCurrentThread();
        long started = System.nanoTime();
        long checksum = task.run();
        long elapsed = System.nanoTime() - started;
        long allocatedAfter = allocatedBytesForCurrentThread();
        consume(checksum);

        long allocated = allocatedBefore < 0L || allocatedAfter < 0L
                ? -1L
                : Math.max(0L, allocatedAfter - allocatedBefore);
        return new Measurement(name, operations, elapsed, allocated, checksum);
    }

    private static void printMeasurements(List<Measurement> measurements) {
        System.out.printf(Locale.ROOT,
                "%-28s %9s %13s %14s %18s%n",
                "implementation",
                "operations",
                "total(ms)",
                "ms/image",
                "allocated(MiB/img)");
        for (Measurement measurement : measurements) {
            String allocationPerImage = measurement.allocatedBytes < 0L
                    ? "N/A"
                    : String.format(Locale.ROOT, "%.4f",
                            bytesToMiB(measurement.allocatedBytes)
                                    / measurement.operations);
            System.out.printf(Locale.ROOT,
                    "%-28s %9d %13.3f %14.4f %18s%n",
                    measurement.name,
                    measurement.operations,
                    measurement.elapsedNanos / 1_000_000.0,
                    measurement.elapsedNanos / 1_000_000.0
                            / measurement.operations,
                    allocationPerImage);
        }
    }

    private static List<Path> collectImages(Path input, int limit)
            throws IOException {
        Path normalized = input.toAbsolutePath().normalize();
        List<Path> found = new ArrayList<>();

        if (Files.isRegularFile(normalized)) {
            if (isSupportedImage(normalized)) {
                found.add(normalized);
            }
        } else if (Files.isDirectory(normalized)) {
            try (Stream<Path> stream = Files.walk(normalized)) {
                stream.filter(Files::isRegularFile)
                        .filter(PatternBenchmark::isSupportedImage)
                        .map(path -> path.toAbsolutePath().normalize())
                        .forEach(found::add);
            }
        }

        found.sort(Comparator.comparing(Path::toString));
        if (found.size() > limit) {
            found = new ArrayList<>(found.subList(0, limit));
        }
        return Collections.unmodifiableList(found);
    }

    private static boolean isSupportedImage(Path path) {
        Path name = path.getFileName();
        if (name == null) {
            return false;
        }
        String lower = name.toString().toLowerCase(Locale.ROOT);
        return lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png");
    }

    private static List<LoadedImage> loadImages(List<Path> paths)
            throws IOException {
        List<LoadedImage> loaded = new ArrayList<>();
        for (Path path : paths) {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
                throw new IOException("Unsupported or damaged image: " + path);
            }
            loaded.add(new LoadedImage(path, image));
        }
        return Collections.unmodifiableList(loaded);
    }

    private static com.sun.management.ThreadMXBean allocationBean() {
        java.lang.management.ThreadMXBean standardBean =
                ManagementFactory.getThreadMXBean();
        if (!(standardBean instanceof com.sun.management.ThreadMXBean)) {
            return null;
        }

        com.sun.management.ThreadMXBean bean =
                (com.sun.management.ThreadMXBean) standardBean;
        try {
            if (!bean.isThreadAllocatedMemorySupported()) {
                return null;
            }
            if (!bean.isThreadAllocatedMemoryEnabled()) {
                bean.setThreadAllocatedMemoryEnabled(true);
            }
            return bean;
        } catch (RuntimeException | LinkageError unavailable) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private static long allocatedBytesForCurrentThread() {
        com.sun.management.ThreadMXBean bean = AllocationBeanHolder.INSTANCE;
        return bean == null
                ? -1L
                : bean.getThreadAllocatedBytes(Thread.currentThread().getId());
    }

    private static void forceGcOutsideTimedSection() throws InterruptedException {
        System.gc();
        Thread.sleep(50L);
    }

    private static void consume(long value) {
        blackhole = 31L * blackhole + value;
    }

    private static double bytesToMiB(long bytes) {
        return bytes / (1024.0 * 1024.0);
    }

    private static void printUsage() {
        System.out.println("Usage: java PatternBenchmark [options] [FILE_OR_DIR]");
        System.out.println("  --input=PATH       input image or directory");
        System.out.println("  --iterations=N    timed repetitions per image (default 3)");
        System.out.println("  --repetitions=N   alias for --iterations");
        System.out.println("  --warmup=N        untimed repetitions (default 2)");
        System.out.println("  --limit=N         maximum input images (default 3)");
        System.out.println("  --help             show this message");
        System.out.println("Cache capacity can be set before startup with:");
        System.out.println("  -Dvariants.factory.cache.maxEntries=N");
    }

    @FunctionalInterface
    private interface CheckedLongTask {
        long run() throws Exception;
    }

    private static final class AllocationBeanHolder {
        private static final com.sun.management.ThreadMXBean INSTANCE =
                allocationBean();
    }

    private static final class LoadedImage {
        private final Path path;
        private final BufferedImage image;

        private LoadedImage(Path path, BufferedImage image) {
            this.path = path;
            this.image = image;
        }
    }

    private static final class Measurement {
        private final String name;
        private final long operations;
        private final long elapsedNanos;
        private final long allocatedBytes;
        private final long checksum;

        private Measurement(
                String name,
                long operations,
                long elapsedNanos,
                long allocatedBytes,
                long checksum) {
            this.name = name;
            this.operations = operations;
            this.elapsedNanos = elapsedNanos;
            this.allocatedBytes = allocatedBytes;
            this.checksum = checksum;
        }
    }

    private static final class BenchmarkOptions {
        private final Path input;
        private final int iterations;
        private final int warmup;
        private final int limit;
        private final boolean help;

        private BenchmarkOptions(
                Path input,
                int iterations,
                int warmup,
                int limit,
                boolean help) {
            this.input = input;
            this.iterations = iterations;
            this.warmup = warmup;
            this.limit = limit;
            this.help = help;
        }

        private static BenchmarkOptions parse(String[] args) {
            Path input = Paths.get("images", "002", "005.jpg");
            int iterations = DEFAULT_ITERATIONS;
            int warmup = DEFAULT_WARMUP;
            int limit = DEFAULT_LIMIT;
            boolean help = false;

            for (String arg : args) {
                if (arg.startsWith("--input=")) {
                    input = requiredPath(arg, "--input=");
                } else if (arg.startsWith("--iterations=")) {
                    iterations = positiveInt(arg, "--iterations=");
                } else if (arg.startsWith("--repetitions=")) {
                    iterations = positiveInt(arg, "--repetitions=");
                } else if (arg.startsWith("--warmup=")) {
                    warmup = nonNegativeInt(arg, "--warmup=");
                } else if (arg.startsWith("--limit=")) {
                    limit = positiveInt(arg, "--limit=");
                } else if ("--help".equals(arg) || "-h".equals(arg)) {
                    help = true;
                } else if (arg.startsWith("--")) {
                    throw new IllegalArgumentException("Unknown option: " + arg);
                } else {
                    input = Paths.get(arg);
                }
            }
            return new BenchmarkOptions(input, iterations, warmup, limit, help);
        }

        private static Path requiredPath(String arg, String prefix) {
            String value = arg.substring(prefix.length());
            if (value.isEmpty()) {
                throw new IllegalArgumentException(prefix + " requires a path");
            }
            return Paths.get(value);
        }

        private static int positiveInt(String arg, String prefix) {
            int value = parseInt(arg, prefix);
            if (value <= 0) {
                throw new IllegalArgumentException(prefix + " must be at least 1");
            }
            return value;
        }

        private static int nonNegativeInt(String arg, String prefix) {
            int value = parseInt(arg, prefix);
            if (value < 0) {
                throw new IllegalArgumentException(prefix + " must be at least 0");
            }
            return value;
        }

        private static int parseInt(String arg, String prefix) {
            String text = arg.substring(prefix.length());
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException invalid) {
                throw new IllegalArgumentException(
                        prefix + " requires an integer: " + text, invalid);
            }
        }
    }
}

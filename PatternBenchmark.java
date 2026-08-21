import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 동일한 Task1Splitter 알고리즘을 Direct, Strategy, Visitor,
 * Abstract Factory + Singleton Cache 구조로 실행해 비교한다.
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
        List<Path> paths = collectImages(options.input, options.limit);
        if (paths.isEmpty()) {
            throw new IllegalArgumentException("처리할 이미지가 없습니다: " + options.input);
        }

        System.out.printf(Locale.ROOT,
                "입력=%s, 이미지=%d장, warmup=%d, iterations=%d%n",
                options.input, paths.size(), options.warmup, options.iterations);

        runAlgorithmPatternBenchmark(paths, options);
        runLoadingAndCacheBenchmark(paths, options);
    }

    private static void runAlgorithmPatternBenchmark(
            List<Path> paths, BenchmarkOptions options) throws Exception {
        WebtoonBatch batch = loadBatch(paths);
        List<WebtoonPage> pages = batch.pages();
        DirectSplitter direct = new DirectSplitter();
        CutPointStrategy strategy = MarginCutPointStrategy.getInstance();

        verifyEquivalentResults(batch, pages, direct, strategy);

        for (int i = 0; i < options.warmup; i++) {
            blackhole ^= runDirect(pages, direct, 1);
            blackhole ^= runStrategy(pages, strategy, 1);
            blackhole ^= runVisitor(batch, strategy, 1);
        }

        int operations = pages.size() * options.iterations;
        List<Measurement> measurements = new ArrayList<>();
        measurements.add(measure(
                "DIRECT",
                operations,
                () -> runDirect(pages, direct, options.iterations)));
        measurements.add(measure(
                "STRATEGY_SINGLETON",
                operations,
                () -> runStrategy(pages, strategy, options.iterations)));
        measurements.add(measure(
                "VISITOR",
                operations,
                () -> runVisitor(batch, strategy, options.iterations)));

        System.out.println();
        System.out.println("[A] 사전 로드된 동일 BufferedImage의 컷 검출 비교");
        printMeasurements(measurements);
        System.out.println("검증: Direct = Strategy = Visitor의 cut Y 결과가 모두 같습니다.");
    }

    private static void runLoadingAndCacheBenchmark(
            List<Path> paths, BenchmarkOptions options) throws Exception {
        ProcessingComponentFactory standardFactory = new StandardProcessingFactory();
        ProcessingComponentFactory cachedFactory = new CachedProcessingFactory();
        WebtoonProcessingService standard = new WebtoonProcessingService(standardFactory);
        WebtoonProcessingService cached = new WebtoonProcessingService(cachedFactory);
        SingletonImageRepository cache = SingletonImageRepository.getInstance();

        if (paths.size() > cache.capacity()) {
            System.out.printf(Locale.ROOT,
                    "주의: 이미지 %d장이 캐시 용량 %d보다 많아 warm 측정에도 LRU miss가 생깁니다.%n"
                            + "      -Dwebtoon.cache.maxEntries=%d 이상으로 실행하면 전부 유지됩니다.%n",
                    paths.size(), cache.capacity(), paths.size());
        }

        // 클래스 로딩과 JIT 영향을 timed 구간 밖으로 보낸다.
        standard.process(paths, 1);
        cache.clear();
        cached.process(paths, 1);
        cache.clear();

        int repeatedOperations = paths.size() * options.iterations;
        Measurement noCache = measure(
                standardFactory.name(),
                repeatedOperations,
                () -> standard.process(paths, options.iterations));

        cache.clear();
        forceGc();
        long heapBeforeCache = usedHeapBytes();
        Measurement cacheCold = measure(
                cachedFactory.name() + "_COLD",
                paths.size(),
                () -> cached.process(paths, 1));
        forceGc();
        long retainedByCache = Math.max(0L, usedHeapBytes() - heapBeforeCache);

        long hitsBeforeWarm = cache.hitCount();
        long missesBeforeWarm = cache.missCount();
        Measurement cacheWarm = measure(
                cachedFactory.name() + "_WARM",
                repeatedOperations,
                () -> cached.process(paths, options.iterations));
        long warmHits = cache.hitCount() - hitsBeforeWarm;
        long warmMisses = cache.missCount() - missesBeforeWarm;

        System.out.println();
        System.out.println("[B] 이미지 로딩 + 컷 검출 비교 (파일 출력 제외)");
        printMeasurements(Arrays.asList(noCache, cacheCold, cacheWarm));
        System.out.printf(Locale.ROOT,
                "Singleton LRU cache: entries=%d/%d, warm hits=%d, warm misses=%d, "
                        + "pixel bytes=%.2f MiB, 추정 retained heap=%.2f MiB%n",
                cache.size(), cache.capacity(), warmHits, warmMisses,
                bytesToMiB(cache.estimatedPixelBytes()),
                bytesToMiB(retainedByCache));
        System.out.println("참고: OS 파일 캐시와 GC 상태 때문에 시간·retained heap은 실행마다 달라집니다.");

        cache.clear();
    }

    private static WebtoonBatch loadBatch(List<Path> paths) throws IOException {
        ImageRepository loader = new DiskImageRepository();
        Map<String, List<WebtoonPage>> pagesByEpisode = new LinkedHashMap<>();

        for (Path path : paths) {
            Path parent = path.getParent();
            String episodeName = parent == null
                    ? "root"
                    : parent.getFileName().toString();
            pagesByEpisode.computeIfAbsent(episodeName, ignored -> new ArrayList<>())
                    .add(new WebtoonPage(path, loader.load(path)));
        }

        List<WebtoonEpisode> episodes = new ArrayList<>();
        for (Map.Entry<String, List<WebtoonPage>> entry : pagesByEpisode.entrySet()) {
            episodes.add(new WebtoonEpisode(entry.getKey(), entry.getValue()));
        }
        return new WebtoonBatch(episodes);
    }

    private static void verifyEquivalentResults(
            WebtoonBatch batch,
            List<WebtoonPage> pages,
            DirectSplitter directSplitter,
            CutPointStrategy strategy) {
        Map<Path, List<Integer>> directResults = new LinkedHashMap<>();

        for (WebtoonPage page : pages) {
            List<Integer> direct = directSplitter.findCutPoints(page.image());
            List<Integer> strategic = strategy.findCutPoints(page.image());
            if (!direct.equals(strategic)) {
                throw new IllegalStateException(
                        "Direct/Strategy 결과 불일치: " + page.path());
            }
            directResults.put(page.path(), direct);
        }

        CutAnalysisVisitor visitor = new CutAnalysisVisitor(strategy);
        batch.accept(visitor);
        if (!directResults.equals(visitor.results())) {
            throw new IllegalStateException("Direct/Visitor 결과가 일치하지 않습니다.");
        }
    }

    private static long runDirect(
            List<WebtoonPage> pages,
            DirectSplitter directSplitter,
            int repetitions) {
        long checksum = 1;
        for (int repetition = 0; repetition < repetitions; repetition++) {
            for (WebtoonPage page : pages) {
                List<Integer> cuts = directSplitter.findCutPoints(page.image());
                checksum = 31 * checksum + cuts.hashCode();
            }
        }
        return checksum;
    }

    private static long runStrategy(
            List<WebtoonPage> pages,
            CutPointStrategy strategy,
            int repetitions) {
        long checksum = 1;
        for (int repetition = 0; repetition < repetitions; repetition++) {
            for (WebtoonPage page : pages) {
                List<Integer> cuts = strategy.findCutPoints(page.image());
                checksum = 31 * checksum + cuts.hashCode();
            }
        }
        return checksum;
    }

    private static long runVisitor(
            WebtoonBatch batch,
            CutPointStrategy strategy,
            int repetitions) {
        long checksum = 1;
        for (int repetition = 0; repetition < repetitions; repetition++) {
            CutAnalysisVisitor visitor = new CutAnalysisVisitor(strategy);
            batch.accept(visitor);
            checksum = 31 * checksum + visitor.checksum();
        }
        return checksum;
    }

    private static Measurement measure(
            String name, int operations, CheckedLongTask task) throws Exception {
        forceGc();
        long allocatedBefore = allocatedBytesForCurrentThread();
        long started = System.nanoTime();
        long checksum = task.run();
        long elapsed = System.nanoTime() - started;
        long allocatedAfter = allocatedBytesForCurrentThread();
        blackhole = checksum;

        long allocated = allocatedBefore < 0 || allocatedAfter < 0
                ? -1
                : Math.max(0, allocatedAfter - allocatedBefore);
        return new Measurement(name, operations, elapsed, allocated, checksum);
    }

    private static void printMeasurements(List<Measurement> measurements) {
        System.out.printf("%-38s %12s %14s %16s%n",
                "구현", "총 시간(ms)", "이미지당(ms)", "할당(MiB/image)");
        for (Measurement measurement : measurements) {
            String allocated = measurement.allocatedBytes < 0
                    ? "N/A"
                    : String.format(Locale.ROOT, "%.3f",
                            bytesToMiB(measurement.allocatedBytes)
                                    / measurement.operations);
            System.out.printf(Locale.ROOT, "%-38s %12.3f %14.3f %16s%n",
                    measurement.name,
                    measurement.elapsedNanos / 1_000_000.0,
                    measurement.elapsedNanos / 1_000_000.0 / measurement.operations,
                    allocated);
        }
    }

    private static List<Path> collectImages(Path input, int limit) {
        List<File> files = new ArrayList<>();
        collectImageFiles(input.toFile(), files);
        files.sort(Comparator.comparing(File::getPath));

        List<Path> paths = new ArrayList<>();
        for (File file : files) {
            if (paths.size() == limit) {
                break;
            }
            paths.add(file.toPath().toAbsolutePath().normalize());
        }
        return paths;
    }

    private static void collectImageFiles(File input, List<File> result) {
        if (input.isFile()) {
            String name = input.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")) {
                result.add(input);
            }
            return;
        }

        File[] children = input.listFiles();
        if (children == null) {
            return;
        }
        Arrays.sort(children, Comparator.comparing(File::getName));
        for (File child : children) {
            collectImageFiles(child, result);
        }
    }

    private static com.sun.management.ThreadMXBean allocationBean() {
        java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        if (!(bean instanceof com.sun.management.ThreadMXBean)) {
            return null;
        }
        com.sun.management.ThreadMXBean allocationBean =
                (com.sun.management.ThreadMXBean) bean;
        if (!allocationBean.isThreadAllocatedMemorySupported()) {
            return null;
        }
        if (!allocationBean.isThreadAllocatedMemoryEnabled()) {
            allocationBean.setThreadAllocatedMemoryEnabled(true);
        }
        return allocationBean;
    }

    @SuppressWarnings("deprecation")
    private static long allocatedBytesForCurrentThread() {
        com.sun.management.ThreadMXBean bean = AllocationBeanHolder.INSTANCE;
        return bean == null
                ? -1
                : bean.getThreadAllocatedBytes(Thread.currentThread().getId());
    }

    private static long usedHeapBytes() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static void forceGc() throws InterruptedException {
        System.gc();
        Thread.sleep(75L);
    }

    private static double bytesToMiB(long bytes) {
        return bytes / (1024.0 * 1024.0);
    }

    @FunctionalInterface
    private interface CheckedLongTask {
        long run() throws Exception;
    }

    private static final class AllocationBeanHolder {
        private static final com.sun.management.ThreadMXBean INSTANCE = allocationBean();
    }

    private static final class Measurement {
        final String name;
        final int operations;
        final long elapsedNanos;
        final long allocatedBytes;
        final long checksum;

        Measurement(
                String name,
                int operations,
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
        final Path input;
        final int iterations;
        final int warmup;
        final int limit;

        BenchmarkOptions(Path input, int iterations, int warmup, int limit) {
            this.input = input;
            this.iterations = iterations;
            this.warmup = warmup;
            this.limit = limit;
        }

        static BenchmarkOptions parse(String[] args) {
            Path input = new File("images/002/005.jpg").toPath();
            int iterations = DEFAULT_ITERATIONS;
            int warmup = DEFAULT_WARMUP;
            int limit = DEFAULT_LIMIT;

            for (String arg : args) {
                if (arg.startsWith("--input=")) {
                    input = new File(arg.substring("--input=".length())).toPath();
                } else if (arg.startsWith("--iterations=")) {
                    iterations = positiveInt(arg, "--iterations=");
                } else if (arg.startsWith("--warmup=")) {
                    warmup = nonNegativeInt(arg, "--warmup=");
                } else if (arg.startsWith("--limit=")) {
                    limit = positiveInt(arg, "--limit=");
                } else if (!arg.startsWith("--")) {
                    input = new File(arg).toPath();
                } else {
                    throw new IllegalArgumentException("알 수 없는 옵션: " + arg);
                }
            }
            return new BenchmarkOptions(input, iterations, warmup, limit);
        }

        private static int positiveInt(String arg, String prefix) {
            int value = Integer.parseInt(arg.substring(prefix.length()));
            if (value <= 0) {
                throw new IllegalArgumentException(prefix + " 값은 1 이상이어야 합니다.");
            }
            return value;
        }

        private static int nonNegativeInt(String arg, String prefix) {
            int value = Integer.parseInt(arg.substring(prefix.length()));
            if (value < 0) {
                throw new IllegalArgumentException(prefix + " 값은 0 이상이어야 합니다.");
            }
            return value;
        }
    }
}

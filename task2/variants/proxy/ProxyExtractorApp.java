package task2.variants.proxy;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

/** Standalone Task2 CLI that demonstrates a bounded caching Proxy. */
public final class ProxyExtractorApp {
    private ProxyExtractorApp() {
    }

    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    /** Testable CLI entry point that does not terminate the JVM. */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        Objects.requireNonNull(args, "args");
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(err, "err");
        try {
            Options options = Options.parse(args);
            if (options.help) {
                printUsage(out);
                return 0;
            }

            List<Path> sources = collectImagePaths(options.inputs);
            if (sources.isEmpty()) {
                err.println("No supported images found in: " + options.inputs);
                return 2;
            }

            ImageIO.setUseCache(false);
            ImageRepository repository = createRepository(
                    options.direct, options.capacity);
            ProxyExtractionService service =
                    new ProxyExtractionService(repository);
            out.printf(Locale.ROOT,
                    "repository=%s detector=%s writer=%s images=%d dryRun=%s%n",
                    options.direct
                            ? "disk-real-subject"
                            : "lru-caching-proxy(capacity="
                                    + options.capacity + ")",
                    service.detectorName(),
                    service.writerName(),
                    sources.size(),
                    options.dryRun);

            int failures = 0;
            for (Path source : sources) {
                try {
                    ExtractionResult result = service.process(
                            source, options.outputRoot, !options.dryRun);
                    out.printf(Locale.ROOT,
                            "%s -> %s%s%n",
                            result.source(),
                            result.regions(),
                            options.dryRun
                                    ? ""
                                    : " (wrote "
                                            + result.writtenFiles().size() + ")");
                } catch (IOException | RuntimeException failure) {
                    failures++;
                    err.println("Failed to process " + source + ": "
                            + failure.getMessage());
                }
            }

            if (repository instanceof CachingImageRepositoryProxy) {
                out.println(((CachingImageRepositoryProxy) repository).stats());
            }
            return failures == 0 ? 0 : 1;
        } catch (IllegalArgumentException | IOException failure) {
            err.println(failure.getMessage());
            printUsage(err);
            return 2;
        }
    }

    public static ImageRepository createRepository(
            boolean direct, int capacity) {
        ImageRepository disk = new DiskImageRepository();
        return direct
                ? disk
                : new CachingImageRepositoryProxy(disk, capacity);
    }

    /** Convenience entry point for comparing the independent detector. */
    public static List<ContentRegion> findContentRegions(
            BufferedImage image) {
        return new RgbMarginContentRegionDetector()
                .findContentRegions(Objects.requireNonNull(image, "image"));
    }

    /** Recursively expands files/directories, removes duplicates, and sorts. */
    public static List<Path> collectImagePaths(List<Path> inputs)
            throws IOException {
        if (inputs == null) {
            throw new IllegalArgumentException("inputs must not be null");
        }
        Set<Path> found = new LinkedHashSet<Path>();
        for (Path input : inputs) {
            if (input == null) {
                continue;
            }
            Path normalized = input.toAbsolutePath().normalize();
            if (Files.isRegularFile(normalized)) {
                if (isSupportedImage(normalized)) {
                    found.add(normalized);
                }
            } else if (Files.isDirectory(normalized)) {
                try (Stream<Path> stream = Files.walk(normalized)) {
                    stream.filter(Files::isRegularFile)
                            .filter(ProxyExtractorApp::isSupportedImage)
                            .map(path -> path.toAbsolutePath().normalize())
                            .forEach(found::add);
                }
            }
        }

        List<Path> sorted = new ArrayList<Path>(found);
        Collections.sort(sorted, Comparator.comparing(Path::toString));
        return Collections.unmodifiableList(sorted);
    }

    private static boolean isSupportedImage(Path path) {
        Path namePath = path.getFileName();
        if (namePath == null) {
            return false;
        }
        String name = namePath.toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".png");
    }

    private static void printUsage(PrintStream out) {
        out.println("Usage: java task2.variants.proxy.ProxyExtractorApp "
                + "[--direct] [--capacity=N] [--dry-run] "
                + "[--output=DIR] [FILE_OR_DIR ...]");
        out.println("Default mode: bounded LRU caching proxy (capacity "
                + CachingImageRepositoryProxy.DEFAULT_CAPACITY + ").");
        out.println("--direct bypasses the proxy and uses the disk repository.");
        out.println("When no input is supplied, ./images is processed recursively.");
        out.println("Default output: ./output-task2-proxy");
    }

    private static final class Options {
        private final boolean direct;
        private final int capacity;
        private final boolean dryRun;
        private final boolean help;
        private final Path outputRoot;
        private final List<Path> inputs;

        private Options(
                boolean direct,
                int capacity,
                boolean dryRun,
                boolean help,
                Path outputRoot,
                List<Path> inputs) {
            this.direct = direct;
            this.capacity = capacity;
            this.dryRun = dryRun;
            this.help = help;
            this.outputRoot = outputRoot;
            this.inputs = inputs;
        }

        private static Options parse(String[] args) {
            boolean direct = false;
            int capacity = CachingImageRepositoryProxy.DEFAULT_CAPACITY;
            boolean dryRun = false;
            boolean help = false;
            Path outputRoot = Paths.get("output-task2-proxy");
            List<Path> inputs = new ArrayList<Path>();

            for (String arg : args) {
                if ("--direct".equals(arg)) {
                    direct = true;
                } else if ("--dry-run".equals(arg)) {
                    dryRun = true;
                } else if ("--help".equals(arg) || "-h".equals(arg)) {
                    help = true;
                } else if (arg.startsWith("--capacity=")) {
                    String value = arg.substring("--capacity=".length());
                    capacity = parseCapacity(value);
                } else if (arg.startsWith("--output=")) {
                    String value = arg.substring("--output=".length());
                    if (value.isEmpty()) {
                        throw new IllegalArgumentException(
                                "--output requires a directory");
                    }
                    outputRoot = Paths.get(value);
                } else if (arg.startsWith("--")) {
                    throw new IllegalArgumentException("Unknown option: " + arg);
                } else {
                    inputs.add(Paths.get(arg));
                }
            }
            if (inputs.isEmpty()) {
                inputs.add(Paths.get("images"));
            }
            return new Options(
                    direct, capacity, dryRun, help, outputRoot, inputs);
        }

        private static int parseCapacity(String value) {
            if (value.isEmpty()) {
                throw new IllegalArgumentException(
                        "--capacity requires a positive integer");
            }
            try {
                int parsed = Integer.parseInt(value);
                if (parsed <= 0) {
                    throw new NumberFormatException();
                }
                return parsed;
            } catch (NumberFormatException failure) {
                throw new IllegalArgumentException(
                        "Invalid --capacity value: " + value);
            }
        }
    }
}

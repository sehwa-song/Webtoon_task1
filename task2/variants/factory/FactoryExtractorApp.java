package task2.variants.factory;

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
import java.util.Set;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

/** Standalone Task2 CLI for Abstract Factory + Singleton LRU Cache. */
public final class FactoryExtractorApp {
    private FactoryExtractorApp() {
    }

    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    /** Testable CLI entry point that does not terminate the JVM. */
    public static int run(String[] args, PrintStream out, PrintStream err) {
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
            ProcessingFamilyFactory factory = createFactory(options.cached);
            WebtoonExtractionService service =
                    new WebtoonExtractionService(factory);
            out.printf(Locale.ROOT,
                    "factory=%s detector=%s writer=%s images=%d dryRun=%s%n",
                    service.factoryName(),
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
                                    : " (wrote " + result.writtenFiles().size() + ")");
                } catch (IOException | RuntimeException failure) {
                    failures++;
                    err.println("Failed to process " + source + ": "
                            + failure.getMessage());
                }
            }

            if (options.cached) {
                out.println(((CachedProcessingFactory) factory).cacheSnapshot());
            }
            return failures == 0 ? 0 : 1;
        } catch (IllegalArgumentException | IOException failure) {
            err.println(failure.getMessage());
            printUsage(err);
            return 2;
        }
    }

    public static ProcessingFamilyFactory createFactory(boolean cached) {
        return cached
                ? new CachedProcessingFactory()
                : new StandardProcessingFactory();
    }

    /** Recursively expands files/directories, removes duplicates, and sorts them. */
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
                            .filter(FactoryExtractorApp::isSupportedImage)
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
        out.println("Usage: java task2.variants.factory.FactoryExtractorApp "
                + "[--cached] [--dry-run] [--output=DIR] [FILE_OR_DIR ...]");
        out.println("When no input is supplied, ./images is processed recursively.");
        out.println("Default output: ./output-task2-factory");
        out.println("Set -Dtask2.variants.factory.cache.maxEntries=N to size the LRU cache.");
    }

    private static final class Options {
        final boolean cached;
        final boolean dryRun;
        final boolean help;
        final Path outputRoot;
        final List<Path> inputs;

        Options(
                boolean cached,
                boolean dryRun,
                boolean help,
                Path outputRoot,
                List<Path> inputs) {
            this.cached = cached;
            this.dryRun = dryRun;
            this.help = help;
            this.outputRoot = outputRoot;
            this.inputs = inputs;
        }

        static Options parse(String[] args) {
            boolean cached = false;
            boolean dryRun = false;
            boolean help = false;
            Path outputRoot = Paths.get("output-task2-factory");
            List<Path> inputs = new ArrayList<Path>();

            for (String arg : args) {
                if ("--cached".equals(arg)) {
                    cached = true;
                } else if ("--dry-run".equals(arg)) {
                    dryRun = true;
                } else if ("--help".equals(arg) || "-h".equals(arg)) {
                    help = true;
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
            return new Options(cached, dryRun, help, outputRoot, inputs);
        }
    }
}

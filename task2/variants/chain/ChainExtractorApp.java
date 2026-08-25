package task2.variants.chain;

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
import java.util.Set;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

/** Standalone Task2 CLI for the Chain of Responsibility variant. */
public final class ChainExtractorApp {
    private static final ChainExtractor DEFAULT_EXTRACTOR =
            new ChainExtractor();

    private ChainExtractorApp() {
    }

    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    /** Preloaded-image entry point for equivalence checks and JMH. */
    public static List<ContentRegion> findContentRegions(BufferedImage image) {
        return DEFAULT_EXTRACTOR.findContentRegions(image);
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
            out.printf(Locale.ROOT,
                    "detector=%s writer=%s images=%d dryRun=%s%n",
                    DEFAULT_EXTRACTOR.detectorName(),
                    DEFAULT_EXTRACTOR.writerName(),
                    sources.size(),
                    options.dryRun);

            int failures = 0;
            for (Path source : sources) {
                try {
                    ExtractionResult result = DEFAULT_EXTRACTOR.process(
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
            return failures == 0 ? 0 : 1;
        } catch (IllegalArgumentException | IOException failure) {
            err.println(failure.getMessage());
            printUsage(err);
            return 2;
        }
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
                            .filter(ChainExtractorApp::isSupportedImage)
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
        out.println("Usage: java task2.variants.chain.ChainExtractorApp "
                + "[--dry-run] [--output=DIR] [FILE_OR_DIR ...]");
        out.println("When no input is supplied, ./images is processed recursively.");
        out.println("Default output: ./output-task2-chain");
    }

    private static final class Options {
        final boolean dryRun;
        final boolean help;
        final Path outputRoot;
        final List<Path> inputs;

        Options(
                boolean dryRun,
                boolean help,
                Path outputRoot,
                List<Path> inputs) {
            this.dryRun = dryRun;
            this.help = help;
            this.outputRoot = outputRoot;
            this.inputs = inputs;
        }

        static Options parse(String[] args) {
            boolean dryRun = false;
            boolean help = false;
            Path outputRoot = Paths.get("output-task2-chain");
            List<Path> inputs = new ArrayList<Path>();

            for (String arg : args) {
                if ("--dry-run".equals(arg)) {
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
            return new Options(dryRun, help, outputRoot, inputs);
        }
    }
}

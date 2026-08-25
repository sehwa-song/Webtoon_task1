package task2.variants.strategy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.imageio.ImageIO;

/** Standalone command-line client for the Task2 Strategy implementation. */
public final class StrategyExtractorApp {
    private static final StrategyExtractor DEFAULT_EXTRACTOR =
            new StrategyExtractor(new MarginContentRegionStrategy());

    private StrategyExtractorApp() {
    }

    /**
     * Usage: {@code [--dry-run] [--output=DIR] [FILE_OR_DIR ...]}.
     * With no input, the application scans {@code images} recursively.
     */
    public static void main(String[] args) {
        boolean dryRun = false;
        File outputRoot = new File("output-task2-strategy");
        List<File> inputs = new ArrayList<File>();

        for (String arg : args) {
            if ("--dry-run".equals(arg)) {
                dryRun = true;
            } else if (arg.startsWith("--output=")) {
                outputRoot = new File(arg.substring("--output=".length()));
            } else {
                inputs.add(new File(arg));
            }
        }
        if (inputs.isEmpty()) {
            inputs.add(new File("images"));
        }

        List<File> imageFiles = new ArrayList<File>();
        for (File input : inputs) {
            collectImageFiles(input, imageFiles);
        }
        Collections.sort(imageFiles, Comparator.comparing(File::getPath));

        if (imageFiles.isEmpty()) {
            System.err.println("No supported image files were found: " + inputs);
            return;
        }

        ImageIO.setUseCache(false);
        for (File imageFile : imageFiles) {
            try {
                List<ContentRegion> regions = extractImage(
                        imageFile,
                        outputRoot,
                        !dryRun,
                        DEFAULT_EXTRACTOR.getStrategy());
                System.out.println(imageFile.getPath() + " -> " + regions);
            } catch (IOException | RuntimeException failure) {
                System.err.println("Failed to process image: " + imageFile.getPath());
                failure.printStackTrace();
            }
        }
    }

    /** Detection-only entry point using the default strategy. */
    public static List<ContentRegion> findContentRegions(BufferedImage image) {
        return DEFAULT_EXTRACTOR.findContentRegions(image);
    }

    /** Detection-only entry point for a client-supplied strategy. */
    public static List<ContentRegion> findContentRegions(
            BufferedImage image, ContentRegionStrategy strategy) {
        return new StrategyExtractor(strategy).findContentRegions(image);
    }

    /** Full-pipeline entry point using the default strategy. */
    public static List<ContentRegion> extractImage(
            File imageFile, File outputRoot, boolean writeFiles) throws IOException {
        return extractImage(
                imageFile,
                outputRoot,
                writeFiles,
                DEFAULT_EXTRACTOR.getStrategy());
    }

    /**
     * Loads one image, delegates detection to the selected strategy, and
     * optionally writes only its content regions.
     */
    public static List<ContentRegion> extractImage(
            File imageFile,
            File outputRoot,
            boolean writeFiles,
            ContentRegionStrategy strategy) throws IOException {
        Objects.requireNonNull(imageFile, "imageFile");
        Objects.requireNonNull(outputRoot, "outputRoot");
        Objects.requireNonNull(strategy, "strategy");

        BufferedImage image = ImageIO.read(imageFile);
        if (image == null) {
            throw new IOException(
                    "Unsupported or unreadable image: " + imageFile.getPath());
        }

        StrategyExtractor extractor = new StrategyExtractor(strategy);
        List<ContentRegion> regions = extractor.findContentRegions(image);
        if (writeFiles) {
            writeRegions(imageFile, image, regions, outputRoot);
        }
        return regions;
    }

    private static void writeRegions(
            File source,
            BufferedImage image,
            List<ContentRegion> regions,
            File outputRoot) throws IOException {
        File parent = source.getParentFile();
        String episode = parent == null ? "root" : parent.getName();
        File outputDirectory = new File(outputRoot, episode);
        if (!outputDirectory.isDirectory() && !outputDirectory.mkdirs()) {
            throw new IOException(
                    "Could not create output directory: " + outputDirectory.getPath());
        }

        String fileName = source.getName();
        int dot = fileName.lastIndexOf('.');
        String baseName = dot < 0 ? fileName : fileName.substring(0, dot);

        for (int index = 0; index < regions.size(); index++) {
            ContentRegion region = regions.get(index);
            BufferedImage piece = copyAsRgb(image, region);
            File output = new File(
                    outputDirectory, baseName + "_content_" + index + ".jpg");
            if (!ImageIO.write(piece, "jpg", output)) {
                throw new IOException("No JPEG writer is available: " + output);
            }
        }
        removeStaleOutputs(outputDirectory, baseName, regions.size());
    }

    private static BufferedImage copyAsRgb(
            BufferedImage source, ContentRegion region) {
        BufferedImage result = new BufferedImage(
                source.getWidth(), region.height(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, result.getWidth(), result.getHeight());
            graphics.drawImage(
                    source,
                    0,
                    0,
                    source.getWidth(),
                    region.height(),
                    0,
                    region.top(),
                    source.getWidth(),
                    region.bottomExclusive(),
                    null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private static void collectImageFiles(File input, List<File> result) {
        if (input.isFile()) {
            if (isSupportedImage(input)) {
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

    private static boolean isSupportedImage(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".png");
    }

    private static void removeStaleOutputs(
            File directory, String baseName, int regionCount) throws IOException {
        String prefix = baseName + "_content_";
        File[] oldFiles = directory.listFiles((ignored, name) ->
                name.startsWith(prefix)
                        && name.toLowerCase(Locale.ROOT).endsWith(".jpg"));
        if (oldFiles == null) {
            return;
        }

        for (File oldFile : oldFiles) {
            String name = oldFile.getName();
            String number = name.substring(prefix.length(), name.length() - 4);
            try {
                int index = Integer.parseInt(number);
                if (index >= regionCount && !oldFile.delete()) {
                    throw new IOException(
                            "Could not delete stale output file: " + oldFile.getPath());
                }
            } catch (NumberFormatException ignored) {
                // Ignore files that do not use this application's numeric suffix.
            }
        }
    }
}

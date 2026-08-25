package variants.strategy;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.imageio.ImageIO;

/** Standalone command-line application for the Strategy experiment. */
public final class StrategySplitterApp {
    private static final StrategySplitter DEFAULT_SPLITTER =
            new StrategySplitter(new MarginCutPointStrategy());

    private StrategySplitterApp() {
    }

    public static void main(String[] args) {
        boolean dryRun = false;
        File outputRoot = new File("output");
        List<File> requestedInputs = new ArrayList<>();

        for (String arg : args) {
            if ("--dry-run".equals(arg)) {
                dryRun = true;
            } else if (arg.startsWith("--output=")) {
                outputRoot = new File(arg.substring("--output=".length()));
            } else {
                requestedInputs.add(new File(arg));
            }
        }

        if (requestedInputs.isEmpty()) {
            requestedInputs.add(new File("images"));
        }

        List<File> imageFiles = new ArrayList<>();
        for (File input : requestedInputs) {
            collectImageFiles(input, imageFiles);
        }
        imageFiles.sort(Comparator.comparing(File::getPath));

        if (imageFiles.isEmpty()) {
            System.err.println("No supported image files were found.");
            return;
        }

        boolean writeFiles = !dryRun;
        for (File imageFile : imageFiles) {
            try {
                List<Integer> cutYs = splitImage(
                        imageFile, outputRoot, writeFiles, DEFAULT_SPLITTER.getStrategy());
                System.out.println(imageFile.getPath() + " -> " + cutYs);
            } catch (IOException | RuntimeException e) {
                System.err.println("Failed to process image: " + imageFile.getPath());
                e.printStackTrace();
            }
        }
    }

    /** Public detection entry point for benchmarks using the default strategy. */
    public static List<Integer> findCutPoints(BufferedImage image) {
        return DEFAULT_SPLITTER.findCutPoints(image);
    }

    /** Public detection entry point for benchmarks supplying another strategy. */
    public static List<Integer> findCutPoints(
            BufferedImage image, CutPointStrategy strategy) {
        return new StrategySplitter(strategy).findCutPoints(image);
    }

    /** Public full-pipeline entry point using the default strategy. */
    public static List<Integer> splitImage(
            File imageFile, File outputRoot, boolean writeFiles) throws IOException {
        return splitImage(imageFile, outputRoot, writeFiles, DEFAULT_SPLITTER.getStrategy());
    }

    /**
     * Public full-pipeline entry point for benchmarks or clients that provide a
     * concrete strategy. When {@code writeFiles} is false, it performs load and
     * detection but does not create output files.
     */
    public static List<Integer> splitImage(
            File imageFile,
            File outputRoot,
            boolean writeFiles,
            CutPointStrategy strategy) throws IOException {
        Objects.requireNonNull(imageFile, "imageFile");
        Objects.requireNonNull(outputRoot, "outputRoot");

        BufferedImage image = ImageIO.read(imageFile);
        if (image == null) {
            throw new IOException("Unsupported or unreadable image: " + imageFile.getPath());
        }

        StrategySplitter splitter = new StrategySplitter(strategy);
        List<Integer> cutYs = splitter.findCutPoints(image);
        if (writeFiles) {
            writePieces(imageFile, image, cutYs, outputRoot);
        }
        return cutYs;
    }

    private static void writePieces(
            File imageFile,
            BufferedImage image,
            List<Integer> cutYs,
            File outputRoot) throws IOException {
        File parent = imageFile.getParentFile();
        String episode = parent == null ? "root" : parent.getName();
        File outputDirectory = new File(outputRoot, episode);
        if (!outputDirectory.isDirectory() && !outputDirectory.mkdirs()) {
            throw new IOException(
                    "Could not create output directory: " + outputDirectory.getPath());
        }

        String fileName = imageFile.getName();
        int dot = fileName.lastIndexOf('.');
        String baseName = dot < 0 ? fileName : fileName.substring(0, dot);

        for (int i = 0; i < cutYs.size() - 1; i++) {
            int top = cutYs.get(i);
            int bottom = cutYs.get(i + 1);
            BufferedImage piece = image.getSubimage(
                    0, top, image.getWidth(), bottom - top);
            File outputFile = new File(
                    outputDirectory, baseName + "_cut_" + i + ".jpg");
            if (!ImageIO.write(piece, "jpg", outputFile)) {
                throw new IOException("No JPEG writer is available.");
            }
        }

        removeStalePieces(outputDirectory, baseName, cutYs.size() - 1);
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

    private static void removeStalePieces(
            File outputDirectory, String baseName, int pieceCount) throws IOException {
        String prefix = baseName + "_cut_";
        File[] oldPieces = outputDirectory.listFiles((directory, name) ->
                name.startsWith(prefix) && name.toLowerCase(Locale.ROOT).endsWith(".jpg"));
        if (oldPieces == null) {
            return;
        }

        for (File oldPiece : oldPieces) {
            String name = oldPiece.getName();
            String indexText = name.substring(prefix.length(), name.length() - 4);
            try {
                int index = Integer.parseInt(indexText);
                if (index >= pieceCount && !oldPiece.delete()) {
                    throw new IOException(
                            "Could not delete stale output file: " + oldPiece.getPath());
                }
            } catch (NumberFormatException ignored) {
                // Ignore files that do not use this application's numeric suffix.
            }
        }
    }
}

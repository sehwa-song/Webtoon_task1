package task2.variants.chain;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/** Writes the content regions accepted by the responsibility chain. */
final class JpegRegionWriter implements RegionWriter {
    JpegRegionWriter() {
    }

    @Override
    public List<Path> writeRegions(
            Path source,
            BufferedImage image,
            List<ContentRegion> regions,
            Path outputRoot) throws IOException {
        validateRegions(image, regions);

        Path normalizedSource = source.toAbsolutePath().normalize();
        Path parent = normalizedSource.getParent();
        String episode = parent == null
                ? "root"
                : parent.getFileName().toString();
        Path outputDirectory = outputRoot.toAbsolutePath().normalize().resolve(episode);
        Files.createDirectories(outputDirectory);

        String fileName = normalizedSource.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String baseName = dot < 0 ? fileName : fileName.substring(0, dot);

        List<Path> written = new ArrayList<Path>();
        for (int index = 0; index < regions.size(); index++) {
            ContentRegion region = regions.get(index);
            BufferedImage piece = copyAsRgb(image, region);
            Path destination = outputDirectory.resolve(
                    baseName + "_content_" + index + ".jpg");
            if (!ImageIO.write(piece, "jpg", destination.toFile())) {
                throw new IOException("No JPEG writer is available: " + destination);
            }
            written.add(destination);
        }

        removeStaleOutputs(outputDirectory, baseName, regions.size());
        return Collections.unmodifiableList(written);
    }

    @Override
    public String name() {
        return "jpeg-content-region-writer";
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

    private static void validateRegions(
            BufferedImage image, List<ContentRegion> regions) {
        if (regions == null) {
            throw new IllegalArgumentException("regions must not be null");
        }
        int previousBottom = -1;
        for (ContentRegion region : regions) {
            if (region == null
                    || region.top() < 0
                    || region.bottomExclusive() > image.getHeight()
                    || region.top() < previousBottom) {
                throw new IllegalArgumentException(
                        "Regions must be ordered, non-overlapping, and in bounds: "
                                + regions);
            }
            previousBottom = region.bottomExclusive();
        }
    }

    private static void removeStaleOutputs(
            Path directory, String baseName, int regionCount) throws IOException {
        String prefix = baseName + "_content_";
        File[] oldFiles = directory.toFile().listFiles((ignored, name) ->
                name.startsWith(prefix)
                        && name.toLowerCase(Locale.ROOT).endsWith(".jpg"));
        if (oldFiles == null) {
            return;
        }
        for (File oldFile : oldFiles) {
            String name = oldFile.getName();
            String indexText = name.substring(prefix.length(), name.length() - 4);
            try {
                int index = Integer.parseInt(indexText);
                if (index >= regionCount && !oldFile.delete()) {
                    throw new IOException("Could not delete stale output: " + oldFile);
                }
            } catch (NumberFormatException ignored) {
                // Ignore similarly named files not created by this writer.
            }
        }
    }
}


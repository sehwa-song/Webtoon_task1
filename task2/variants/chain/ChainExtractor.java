package task2.variants.chain;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.imageio.ImageIO;

/** Orchestrates loading, the handler chain, and optional JPEG writing. */
public final class ChainExtractor {
    private final ChainContentRegionDetector detector;
    private final RegionWriter writer;

    public ChainExtractor() {
        detector = new ChainContentRegionDetector();
        writer = new JpegRegionWriter();
    }

    public List<ContentRegion> findContentRegions(BufferedImage image) {
        return detector.findContentRegions(
                Objects.requireNonNull(image, "image"));
    }

    public ExtractionResult process(
            Path source,
            Path outputRoot,
            boolean writeFiles) throws IOException {
        Objects.requireNonNull(source, "source");
        if (writeFiles) {
            Objects.requireNonNull(outputRoot, "outputRoot");
        }

        Path normalized = source.toAbsolutePath().normalize();
        BufferedImage image = ImageIO.read(normalized.toFile());
        if (image == null) {
            throw new IOException("Unsupported or damaged image: " + normalized);
        }

        List<ContentRegion> regions = detector.findContentRegions(image);
        List<Path> written = writeFiles
                ? writer.writeRegions(normalized, image, regions, outputRoot)
                : Collections.<Path>emptyList();
        return new ExtractionResult(
                normalized, regions, written, detector.name());
    }

    public String detectorName() {
        return detector.name();
    }

    public String writerName() {
        return writer.name();
    }
}

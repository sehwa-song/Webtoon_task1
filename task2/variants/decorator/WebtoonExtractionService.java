package task2.variants.decorator;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.imageio.ImageIO;

/** Executes image loading, decorated detection, and optional JPEG writing. */
public final class WebtoonExtractionService {
    private final ContentRegionDetector detector;
    private final JpegRegionWriter writer;

    public WebtoonExtractionService(ContentRegionDetector detector) {
        this.detector = Objects.requireNonNull(detector, "detector");
        this.writer = new JpegRegionWriter();
    }

    public ExtractionResult process(
            Path source, Path outputRoot, boolean writeFiles) throws IOException {
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
        return new ExtractionResult(normalized, regions, written);
    }

    public String detectorName() {
        return detector.name();
    }
}

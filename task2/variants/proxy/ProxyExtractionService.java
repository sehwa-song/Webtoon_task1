package task2.variants.proxy;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Client in the Proxy pattern. It talks only to {@link ImageRepository}; the
 * same service works with either the RealSubject or the caching Proxy.
 */
public final class ProxyExtractionService {
    private final ImageRepository imageRepository;
    private final ContentRegionDetector detector;
    private final RegionWriter writer;

    public ProxyExtractionService(ImageRepository imageRepository) {
        this(
                imageRepository,
                new RgbMarginContentRegionDetector(),
                new JpegRegionWriter());
    }

    public ProxyExtractionService(
            ImageRepository imageRepository,
            ContentRegionDetector detector,
            RegionWriter writer) {
        this.imageRepository = Objects.requireNonNull(
                imageRepository, "imageRepository");
        this.detector = Objects.requireNonNull(detector, "detector");
        this.writer = Objects.requireNonNull(writer, "writer");
    }

    public ExtractionResult process(
            Path source, Path outputRoot, boolean writeFiles)
            throws IOException {
        Objects.requireNonNull(source, "source");
        if (writeFiles) {
            Objects.requireNonNull(outputRoot, "outputRoot");
        }

        Path normalized = source.toAbsolutePath().normalize();
        BufferedImage image = imageRepository.load(normalized);
        List<ContentRegion> regions = detector.findContentRegions(image);
        List<Path> written = writeFiles
                ? writer.writeRegions(normalized, image, regions, outputRoot)
                : Collections.<Path>emptyList();
        return new ExtractionResult(normalized, regions, written);
    }

    /** Loads through the Subject and then performs content detection. */
    public List<ContentRegion> detect(Path source) throws IOException {
        Objects.requireNonNull(source, "source");
        BufferedImage image = imageRepository.load(
                source.toAbsolutePath().normalize());
        return detector.findContentRegions(image);
    }

    /** Preloaded entry point used for algorithm-only equivalence checks. */
    public List<ContentRegion> detect(BufferedImage image) {
        return detector.findContentRegions(
                Objects.requireNonNull(image, "image"));
    }

    public ImageRepository imageRepository() {
        return imageRepository;
    }

    public String detectorName() {
        return detector.name();
    }

    public String writerName() {
        return writer.name();
    }
}

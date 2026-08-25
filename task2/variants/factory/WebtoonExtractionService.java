package task2.variants.factory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Client that knows only the three Abstract Factory product interfaces. */
public final class WebtoonExtractionService {
    private final String factoryName;
    private final ImageRepository imageRepository;
    private final ContentRegionDetector detector;
    private final RegionWriter writer;

    public WebtoonExtractionService(ProcessingFamilyFactory factory) {
        Objects.requireNonNull(factory, "factory");
        factoryName = Objects.requireNonNull(factory.name(), "factory.name()");
        imageRepository = Objects.requireNonNull(
                factory.createImageRepository(), "factory image repository");
        detector = Objects.requireNonNull(
                factory.createContentRegionDetector(), "factory content detector");
        writer = Objects.requireNonNull(
                factory.createRegionWriter(), "factory region writer");
    }

    public ExtractionResult process(
            Path source, Path outputRoot, boolean writeFiles) throws IOException {
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
        return new ExtractionResult(normalized, regions, written, factoryName);
    }

    /** Loads through the chosen repository and detects through its factory product. */
    public List<ContentRegion> detect(Path source) throws IOException {
        Objects.requireNonNull(source, "source");
        BufferedImage image = imageRepository.load(
                source.toAbsolutePath().normalize());
        return detector.findContentRegions(image);
    }

    /** Preloaded entry point used for fair algorithm-only comparisons. */
    public List<ContentRegion> detect(BufferedImage image) {
        return detector.findContentRegions(
                Objects.requireNonNull(image, "image"));
    }

    public long benchmark(List<Path> sources, int repetitions) throws IOException {
        Objects.requireNonNull(sources, "sources");
        if (repetitions <= 0) {
            throw new IllegalArgumentException("repetitions must be positive");
        }

        long checksum = 1L;
        for (int repetition = 0; repetition < repetitions; repetition++) {
            for (Path source : sources) {
                List<ContentRegion> regions = detect(source);
                checksum = 31L * checksum + regions.hashCode();
            }
        }
        return checksum;
    }

    public String factoryName() {
        return factoryName;
    }

    public String detectorName() {
        return detector.name();
    }

    public String writerName() {
        return writer.name();
    }
}

package variants.factory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Application service that knows only Abstract Factory product interfaces.
 * Construction itself exercises all three factory methods.
 */
public final class WebtoonProcessingService {
    private final String factoryName;
    private final ImageRepository imageRepository;
    private final CutPointDetector cutPointDetector;
    private final PieceWriter pieceWriter;

    public WebtoonProcessingService(ProcessingFamilyFactory factory) {
        Objects.requireNonNull(factory, "factory");
        factoryName = Objects.requireNonNull(factory.name(), "factory.name()");
        imageRepository = Objects.requireNonNull(
                factory.createImageRepository(), "factory image repository");
        cutPointDetector = Objects.requireNonNull(
                factory.createCutPointDetector(), "factory cut detector");
        pieceWriter = Objects.requireNonNull(
                factory.createPieceWriter(), "factory piece writer");
    }

    public ProcessingResult process(
            Path source, Path outputRoot, boolean writePieces) throws IOException {
        Objects.requireNonNull(source, "source");
        if (writePieces) {
            Objects.requireNonNull(outputRoot, "outputRoot");
        }

        Path normalized = source.toAbsolutePath().normalize();
        BufferedImage image = imageRepository.load(normalized);
        List<Integer> cuts = cutPointDetector.findCutPoints(image);
        List<Path> written = writePieces
                ? pieceWriter.writePieces(normalized, image, cuts, outputRoot)
                : Collections.<Path>emptyList();
        return new ProcessingResult(normalized, cuts, written, factoryName);
    }

    /** Loads through the selected repository and detects through the factory product. */
    public List<Integer> detect(Path source) throws IOException {
        Objects.requireNonNull(source, "source");
        BufferedImage image = imageRepository.load(
                source.toAbsolutePath().normalize());
        return cutPointDetector.findCutPoints(image);
    }

    /**
     * Benchmark workload. It deliberately follows factory -> service ->
     * repository -> detector on every operation and never calls a shortcut in
     * the default package.
     */
    public long benchmark(List<Path> sources, int repetitions) throws IOException {
        Objects.requireNonNull(sources, "sources");
        if (repetitions <= 0) {
            throw new IllegalArgumentException("repetitions must be positive");
        }

        long checksum = 1L;
        for (int repetition = 0; repetition < repetitions; repetition++) {
            for (Path source : sources) {
                List<Integer> cuts = detect(source);
                checksum = 31L * checksum + cuts.hashCode();
            }
        }
        return checksum;
    }

    public String factoryName() {
        return factoryName;
    }

    public String detectorName() {
        return cutPointDetector.name();
    }

    public String writerName() {
        return pieceWriter.name();
    }
}

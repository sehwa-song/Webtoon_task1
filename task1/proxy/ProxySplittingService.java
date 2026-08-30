package task1.proxy;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * 서비스는 Disk/Cache 구현을 구분하지 않고 ImageRepository 인터페이스만 사용합니다.
 */
public final class ProxySplittingService {
    private final ImageRepository repository;
    private final CutPointDetector detector;
    private final JpegPieceWriter writer;
    private final File outputRoot;
    private final boolean dryRun;

    public ProxySplittingService(ImageRepository repository,
            CutPointDetector detector, JpegPieceWriter writer,
            File outputRoot, boolean dryRun) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.detector = Objects.requireNonNull(detector, "detector");
        this.writer = Objects.requireNonNull(writer, "writer");
        this.outputRoot = Objects.requireNonNull(outputRoot, "outputRoot");
        this.dryRun = dryRun;
    }

    public ProxyProcessingResult process(File imageFile) throws IOException {
        long totalStart = System.nanoTime();

        long start = System.nanoTime();
        BufferedImage image = repository.read(imageFile);
        long loadNanos = System.nanoTime() - start;

        start = System.nanoTime();
        List<Integer> points = detector.findCutPoints(image);
        long detectNanos = System.nanoTime() - start;

        long writeNanos = 0;
        if (!dryRun) {
            start = System.nanoTime();
            writer.write(imageFile, image, points, outputRoot);
            writeNanos = System.nanoTime() - start;
        }

        return new ProxyProcessingResult(points, loadNanos, detectNanos,
                writeNanos, System.nanoTime() - totalStart);
    }
}

package task1.factory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** 팩토리가 만든 제품만 사용하므로 구체 클래스와 결합되지 않는 Client입니다. */
public final class WebtoonProcessingService {
    private final ImageRepository repository;
    private final CutPointDetector detector;
    private final PieceWriter writer;

    public WebtoonProcessingService(ProcessingFamilyFactory factory) {
        repository = factory.createImageRepository();
        detector = factory.createCutPointDetector();
        writer = factory.createPieceWriter();
    }

    public ProcessingResult process(
            File source,
            File outputRoot,
            boolean dryRun) throws IOException {
        long totalStart = System.nanoTime();

        long loadStart = System.nanoTime();
        BufferedImage image = repository.read(source);
        long loadNanos = System.nanoTime() - loadStart;

        long detectStart = System.nanoTime();
        List<Integer> cuts = detector.findCutPoints(image);
        long detectNanos = System.nanoTime() - detectStart;

        long writeNanos = 0;
        if (!dryRun) {
            long writeStart = System.nanoTime();
            writer.write(source, image, cuts, outputRoot);
            writeNanos = System.nanoTime() - writeStart;
        }

        return new ProcessingResult(
                source,
                new ArrayList<>(cuts),
                loadNanos,
                detectNanos,
                writeNanos,
                System.nanoTime() - totalStart);
    }

    public String products() {
        return repository.name() + " + " + detector.name() + " + " + writer.name();
    }
}

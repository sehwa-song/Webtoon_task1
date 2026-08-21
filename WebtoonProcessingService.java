import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Factory가 만든 구성 요소만 알고 실제 구현 클래스에는 의존하지 않는다. */
final class WebtoonProcessingService {
    private final ImageRepository imageRepository;
    private final CutPointStrategy cutPointStrategy;

    WebtoonProcessingService(ProcessingComponentFactory factory) {
        imageRepository = factory.createImageRepository();
        cutPointStrategy = factory.createCutPointStrategy();
    }

    long process(List<Path> paths, int repetitions) throws IOException {
        long checksum = 1;
        for (int repetition = 0; repetition < repetitions; repetition++) {
            for (Path path : paths) {
                BufferedImage image = imageRepository.load(path);
                List<Integer> cuts = cutPointStrategy.findCutPoints(image);
                checksum = 31 * checksum + cuts.hashCode();
            }
        }
        return checksum;
    }
}

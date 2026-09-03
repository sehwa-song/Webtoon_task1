package task2.strategy;

import java.awt.image.BufferedImage;
import java.util.List;
import task2.core.ContentAlgorithm;
import task2.core.ContentRegion;
import task2.core.SimpleContentAlgorithm;
import task2.core.Task2Runner;

public final class StrategyExample {
    interface ContentStrategy {
        List<ContentRegion> detect(BufferedImage image);
    }

    static final class BlankRemovalStrategy implements ContentStrategy {
        private final ContentAlgorithm algorithm = new SimpleContentAlgorithm();
        public List<ContentRegion> detect(BufferedImage image) { return algorithm.detectContent(image); }
    }

    static final class ContentContext implements ContentAlgorithm {
        private final ContentStrategy strategy;
        ContentContext(ContentStrategy strategy) { this.strategy = strategy; }
        public List<ContentRegion> detectContent(BufferedImage image) { return strategy.detect(image); }
    }

    public static void main(String[] args) throws Exception {
        Task2Runner.run("Strategy", new ContentContext(new BlankRemovalStrategy()), args);
    }
}

package task1.strategy;

import java.awt.image.BufferedImage;
import java.util.List;
import task1.core.CutAlgorithm;
import task1.core.SimpleCutAlgorithm;
import task1.core.Task1Runner;

public final class StrategyExample {
    interface CutStrategy {
        List<Integer> detect(BufferedImage image);
    }

    static final class BlankCutStrategy implements CutStrategy {
        private final CutAlgorithm algorithm = new SimpleCutAlgorithm();
        public List<Integer> detect(BufferedImage image) { return algorithm.detectCuts(image); }
    }

    static final class CutContext implements CutAlgorithm {
        private final CutStrategy strategy;
        CutContext(CutStrategy strategy) { this.strategy = strategy; }
        public List<Integer> detectCuts(BufferedImage image) { return strategy.detect(image); }
    }

    public static void main(String[] args) throws Exception {
        Task1Runner.run("Strategy", new CutContext(new BlankCutStrategy()), args);
    }
}

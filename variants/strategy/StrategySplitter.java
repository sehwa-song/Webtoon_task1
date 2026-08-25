package variants.strategy;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;

/**
 * Context in the Strategy pattern. It knows only the strategy interface and
 * does not contain a concrete cut-detection algorithm.
 */
public final class StrategySplitter { // (전략 등록/실행) 
    private final CutPointStrategy strategy; // 합성

    public StrategySplitter(CutPointStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    public List<Integer> findCutPoints(BufferedImage image) {
        return strategy.findCutPoints(Objects.requireNonNull(image, "image"));
    }

    public CutPointStrategy getStrategy() {
        return strategy;
    }
}

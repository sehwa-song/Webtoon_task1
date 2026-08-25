package task2.variants.strategy;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;

/**
 * Context in the Strategy pattern. It delegates content-region detection to
 * the strategy supplied by the client.
 */
public final class StrategyExtractor {
    private final ContentRegionStrategy strategy;

    public StrategyExtractor(ContentRegionStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    public List<ContentRegion> findContentRegions(BufferedImage image) {
        return strategy.findContentRegions(Objects.requireNonNull(image, "image"));
    }

    public ContentRegionStrategy getStrategy() {
        return strategy;
    }
}

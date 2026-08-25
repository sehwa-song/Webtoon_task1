package task2.variants.chain;

import java.awt.image.BufferedImage;

/** State passed from one removal-candidate handler to the next. */
final class RemovalCandidate {
    final BufferedImage image;
    final ChainContentRegionDetector.RowAnalysis rows;
    final ChainContentRegionDetector.SeparatorZone separator;
    final boolean[] safeBlank;
    final double scale;
    final ChainContentRegionDetector.RemovalZone removal;

    RemovalCandidate(
            BufferedImage image,
            ChainContentRegionDetector.RowAnalysis rows,
            ChainContentRegionDetector.SeparatorZone separator,
            boolean[] safeBlank,
            double scale,
            ChainContentRegionDetector.RemovalZone removal) {
        this.image = image;
        this.rows = rows;
        this.separator = separator;
        this.safeBlank = safeBlank;
        this.scale = scale;
        this.removal = removal;
    }

    RemovalCandidate withRemoval(
            ChainContentRegionDetector.RemovalZone adjustedRemoval) {
        return new RemovalCandidate(
                image,
                rows,
                separator,
                safeBlank,
                scale,
                adjustedRemoval);
    }
}

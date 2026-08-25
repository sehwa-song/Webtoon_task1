package task2.variants.strategy;

import java.awt.image.BufferedImage;
import java.util.List;

/** Strategy contract for finding the non-gutter regions of one webtoon image. */
public interface ContentRegionStrategy {
    List<ContentRegion> findContentRegions(BufferedImage image);
}

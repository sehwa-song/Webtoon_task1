package task2.variants.chain;

import java.awt.image.BufferedImage;
import java.util.List;

/** Detection contract implemented by the chain-based detector. */
public interface ContentRegionDetector {
    List<ContentRegion> findContentRegions(BufferedImage image);

    String name();
}


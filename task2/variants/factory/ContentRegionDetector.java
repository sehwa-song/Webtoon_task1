package task2.variants.factory;

import java.awt.image.BufferedImage;
import java.util.List;

/** Abstract product that finds vertical content ranges after removing gutters. */
public interface ContentRegionDetector {
    List<ContentRegion> findContentRegions(BufferedImage image);

    String name();
}

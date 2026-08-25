package task2.variants.decorator;

import java.awt.image.BufferedImage;
import java.util.List;

/** Component in the Decorator pattern. */
public interface ContentRegionDetector {
    List<ContentRegion> findContentRegions(BufferedImage image);

    String name();
}

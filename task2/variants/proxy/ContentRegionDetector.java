package task2.variants.proxy;

import java.awt.image.BufferedImage;
import java.util.List;

/** Detector contract used by the Proxy client after an image is loaded. */
public interface ContentRegionDetector {
    List<ContentRegion> findContentRegions(BufferedImage image);

    String name();
}


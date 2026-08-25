package variants.factory;

import java.awt.image.BufferedImage;
import java.util.List;

/** Detects ordered Y coordinates, including zero and the image height. */
public interface CutPointDetector {
    List<Integer> findCutPoints(BufferedImage image);

    String name();
}

package task2.core;

import java.awt.image.BufferedImage;
import java.util.List;

@FunctionalInterface
public interface ContentAlgorithm {
    List<ContentRegion> detectContent(BufferedImage image);
}

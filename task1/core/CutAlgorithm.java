package task1.core;

import java.awt.image.BufferedImage;
import java.util.List;

@FunctionalInterface
public interface CutAlgorithm {
    List<Integer> detectCuts(BufferedImage image);
}

package task1.factory;

import java.awt.image.BufferedImage;
import java.util.List;

/** Abstract Factory가 생성하는 컷 위치 탐색 제품입니다. */
public interface CutPointDetector {
    List<Integer> findCutPoints(BufferedImage image);
    String name();
}

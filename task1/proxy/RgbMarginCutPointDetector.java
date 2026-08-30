package task1.proxy;

import java.awt.image.BufferedImage;
import java.util.List;

/** 이 패키지 내부 RGB 알고리즘으로 컷 위치를 찾습니다. */
public final class RgbMarginCutPointDetector implements CutPointDetector {
    @Override
    public List<Integer> findCutPoints(BufferedImage image) {
        return AdaptiveRgbCutAlgorithm.findCutPoints(image);
    }
}

package task1.decorator;

import java.awt.image.BufferedImage;
import java.util.List;

/** Decorator가 감싸는 원본 Component입니다. 실제 RGB 여백 분석만 담당합니다. */
public final class RgbMarginCutPointDetector implements CutPointDetector {
    @Override
    public List<Integer> findCutPoints(BufferedImage image) {
        return AdaptiveRgbCutAlgorithm.findCutPoints(image);
    }

    @Override
    public String name() {
        return "adaptive-rgb-margin";
    }
}

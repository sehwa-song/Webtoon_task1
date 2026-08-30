package task1.strategy;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * 고정 흰색·검은색 판정을 먼저 사용하고, 실패하면 좌우 배경색 판정을 사용하는
 * Concrete Strategy입니다. 현재 Task1의 기본 전략입니다.
 */
public final class HybridRowClassifier implements RowClassifier {
    private final FixedColorRowClassifier fixed =
            new FixedColorRowClassifier();
    private final EdgeColorRowClassifier edge =
            new EdgeColorRowClassifier();

    @Override
    public RowType classify(BufferedImage image, int y) {
        RowType fixedResult = fixed.classify(image, y);
        if (fixedResult == RowType.NEUTRAL_BLANK) {
            return fixedResult;
        }
        return edge.classify(image, y);
    }

    @Override
    public int countForegroundPixels(BufferedImage image, int y) {
        return Math.min(
                fixed.countForegroundPixels(image, y),
                edge.countForegroundPixels(image, y));
    }

    @Override
    public Color findExpectedBackgroundColor(BufferedImage image, int y) {
        return edge.findExpectedBackgroundColor(image, y);
    }

    @Override
    public String name() {
        return "hybrid-row-classifier";
    }
}

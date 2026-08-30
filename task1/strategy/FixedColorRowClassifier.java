package task1.strategy;

import java.awt.Color;
import java.awt.image.BufferedImage;

/** 거의 흰색 또는 거의 검은색인 행만 여백으로 보는 Concrete Strategy입니다. */
public final class FixedColorRowClassifier implements RowClassifier {
    @Override
    public RowType classify(BufferedImage image, int y) {
        int foreground = countForegroundPixels(image, y);
        int maximum = (int) (
                image.getWidth()
                * RgbRowSupport.NEUTRAL_FOREGROUND_RATIO);

        return foreground <= maximum
                ? RowType.NEUTRAL_BLANK
                : RowType.CONTENT;
    }

    @Override
    public int countForegroundPixels(BufferedImage image, int y) {
        return RgbRowSupport.countWhiteOrBlackForegroundPixels(image, y);
    }

    @Override
    public Color findExpectedBackgroundColor(BufferedImage image, int y) {
        // 이 전략은 컬러 여백을 만들지 않으므로 색 안정성 검사에 사용되지 않습니다.
        return null;
    }

    @Override
    public String name() {
        return "fixed-color-row-classifier";
    }
}

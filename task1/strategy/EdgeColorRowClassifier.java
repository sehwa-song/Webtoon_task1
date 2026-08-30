package task1.strategy;

import java.awt.Color;
import java.awt.image.BufferedImage;

/** 좌우 끝에서 예상 배경색을 찾아 행 전체와 비교하는 Concrete Strategy입니다. */
public final class EdgeColorRowClassifier implements RowClassifier {
    @Override
    public RowType classify(BufferedImage image, int y) {
        Color background = findExpectedBackgroundColor(image, y);
        if (background == null) {
            return RowType.CONTENT;
        }

        int foreground = RgbRowSupport.countPixelsDifferentFromBackground(
                image,
                y,
                background);

        if (RgbRowSupport.isNeutralColor(background)) {
            int maximum = (int) (
                    image.getWidth()
                    * RgbRowSupport.NEUTRAL_FOREGROUND_RATIO);
            return foreground <= maximum
                    ? RowType.NEUTRAL_BLANK
                    : RowType.CONTENT;
        }

        int maximum = (int) (
                image.getWidth()
                * RgbRowSupport.COLORED_FOREGROUND_RATIO);
        return foreground <= maximum
                ? RowType.COLORED_BLANK
                : RowType.CONTENT;
    }

    @Override
    public int countForegroundPixels(BufferedImage image, int y) {
        return RgbRowSupport.countEdgeBasedForegroundPixels(image, y);
    }

    @Override
    public Color findExpectedBackgroundColor(BufferedImage image, int y) {
        return RgbRowSupport.findExpectedEdgeBackgroundColor(image, y);
    }

    @Override
    public String name() {
        return "edge-color-row-classifier";
    }
}

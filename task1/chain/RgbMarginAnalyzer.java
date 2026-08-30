package task1.chain;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Handler들이 공동으로 사용하는 순수 RGB 행 분석기입니다.
 * 다른 패키지나 Task1LearningBaseline을 호출하지 않습니다.
 */
public final class RgbMarginAnalyzer {
    static final int CONTENT_ROW = 0;
    static final int NEUTRAL_BLANK_ROW = 1;
    static final int COLORED_BLANK_ROW = 2;

    private static final double EDGE_SAMPLE_RATIO = 0.05;
    private static final double BACKGROUND_COLOR_DISTANCE = 18.0;
    private static final double MAX_EDGE_COLOR_DISTANCE = 18.0;
    private static final double MAX_EDGE_STANDARD_DEVIATION = 8.0;
    private static final int MAX_NEUTRAL_CHANNEL_RANGE = 18;
    private static final double NEUTRAL_FOREGROUND_RATIO = 0.02;
    private static final double COLORED_FOREGROUND_RATIO = 0.005;
    private static final int MIN_NEUTRAL_BLANK_HEIGHT = 50;
    private static final int MIN_COLORED_BLANK_HEIGHT = 100;
    private static final double MAX_COLORED_BACKGROUND_DRIFT = 10.0;

    int getMinimumNeutralBlankHeight() {
        return MIN_NEUTRAL_BLANK_HEIGHT;
    }

    int getMinimumColoredBlankHeight() {
        return MIN_COLORED_BLANK_HEIGHT;
    }

    /** 한 행을 그림/무채색 여백/컬러 여백으로 구분합니다. */
    int classifyBlankRow(BufferedImage image, int y) {
        int width = image.getWidth();
        int neutralMaximum =
                (int) (width * NEUTRAL_FOREGROUND_RATIO);
        int coloredMaximum =
                (int) (width * COLORED_FOREGROUND_RATIO);

        int fixedForeground =
                countWhiteOrBlackForegroundPixels(image, y);
        if (fixedForeground <= neutralMaximum) {
            return NEUTRAL_BLANK_ROW;
        }

        Color background = findExpectedEdgeBackgroundColor(image, y);
        if (background == null) {
            return CONTENT_ROW;
        }

        int foreground = countPixelsDifferentFromBackground(
                image, y, background);
        if (isNeutralColor(background)) {
            return foreground <= neutralMaximum
                    ? NEUTRAL_BLANK_ROW
                    : CONTENT_ROW;
        }
        return foreground <= coloredMaximum
                ? COLORED_BLANK_ROW
                : CONTENT_ROW;
    }

    /** 컬러 후보가 세로 방향으로 거의 같은 색인지 검사합니다. */
    boolean isStableColoredBackground(
            BufferedImage image,
            int start,
            int end) {

        int middle = start + (end - start) / 2;
        Color reference = findExpectedEdgeBackgroundColor(image, middle);
        if (reference == null || isNeutralColor(reference)) {
            return false;
        }

        for (int y = start; y <= end; y++) {
            Color current = findExpectedEdgeBackgroundColor(image, y);
            if (current == null || isNeutralColor(current)
                    || colorDistance(current, reference)
                            > MAX_COLORED_BACKGROUND_DRIFT) {
                return false;
            }
        }
        return true;
    }

    /** 중앙 부근에서 말풍선과 글자 픽셀이 가장 적은 안전한 y를 고릅니다. */
    int findSafestCutY(
            BufferedImage image,
            int start,
            int end) {

        int radius = 5;
        int middle = start + (end - start) / 2;
        int maximumForeground = Math.max(
                1,
                (int) (image.getWidth() * COLORED_FOREGROUND_RATIO));
        int bestY = -1;
        long bestScore = Long.MAX_VALUE;

        for (int y = start + radius; y <= end - radius; y++) {
            long score = 0;
            boolean safe = true;

            for (int checkY = y - radius;
                    checkY <= y + radius;
                    checkY++) {
                int foreground = countForegroundPixels(image, checkY);
                score += foreground;
                if (foreground > maximumForeground) {
                    safe = false;
                    break;
                }
            }

            if (!safe) {
                continue;
            }

            boolean lowerScore = score < bestScore;
            boolean closer = bestY != -1
                    && score == bestScore
                    && Math.abs(y - middle) < Math.abs(bestY - middle);
            if (lowerScore || closer) {
                bestY = y;
                bestScore = score;
            }
        }
        return bestY;
    }

    private static int countForegroundPixels(
            BufferedImage image,
            int y) {

        return Math.min(
                countWhiteOrBlackForegroundPixels(image, y),
                countEdgeBasedForegroundPixels(image, y));
    }

    private static int countWhiteOrBlackForegroundPixels(
            BufferedImage image,
            int y) {

        int whiteCount = 0;
        int blackCount = 0;

        for (int x = 0; x < image.getWidth(); x++) {
            Color color = new Color(image.getRGB(x, y));
            boolean white = color.getRed() >= 245
                    && color.getGreen() >= 245
                    && color.getBlue() >= 245;
            boolean black = color.getRed() <= 20
                    && color.getGreen() <= 20
                    && color.getBlue() <= 20;

            if (white) {
                whiteCount++;
            }
            if (black) {
                blackCount++;
            }
        }
        return image.getWidth() - Math.max(whiteCount, blackCount);
    }

    private static int countEdgeBasedForegroundPixels(
            BufferedImage image,
            int y) {

        Color background = findExpectedEdgeBackgroundColor(image, y);
        if (background == null) {
            return image.getWidth();
        }
        return countPixelsDifferentFromBackground(image, y, background);
    }

    private static Color findExpectedEdgeBackgroundColor(
            BufferedImage image,
            int y) {

        int width = image.getWidth();
        int edgeWidth = Math.max(
                1,
                (int) Math.round(width * EDGE_SAMPLE_RATIO));
        edgeWidth = Math.min(edgeWidth, Math.max(1, width / 3));

        Color left = averageColor(image, y, 0, edgeWidth);
        Color right = averageColor(
                image, y, width - edgeWidth, width);

        if (colorDistance(left, right) > MAX_EDGE_COLOR_DISTANCE) {
            return null;
        }

        double leftDeviation = colorStandardDeviation(
                image, y, 0, edgeWidth, left);
        double rightDeviation = colorStandardDeviation(
                image, y, width - edgeWidth, width, right);
        if (leftDeviation > MAX_EDGE_STANDARD_DEVIATION
                || rightDeviation > MAX_EDGE_STANDARD_DEVIATION) {
            return null;
        }

        return new Color(
                (left.getRed() + right.getRed()) / 2,
                (left.getGreen() + right.getGreen()) / 2,
                (left.getBlue() + right.getBlue()) / 2);
    }

    private static boolean isNeutralColor(Color color) {
        int maximum = Math.max(
                color.getRed(),
                Math.max(color.getGreen(), color.getBlue()));
        int minimum = Math.min(
                color.getRed(),
                Math.min(color.getGreen(), color.getBlue()));
        return maximum - minimum <= MAX_NEUTRAL_CHANNEL_RANGE;
    }

    private static int countPixelsDifferentFromBackground(
            BufferedImage image,
            int y,
            Color background) {

        int count = 0;
        double maximumDistanceSquared =
                BACKGROUND_COLOR_DISTANCE * BACKGROUND_COLOR_DISTANCE;
        for (int x = 0; x < image.getWidth(); x++) {
            Color current = new Color(image.getRGB(x, y));
            if (colorDistanceSquared(current, background)
                    > maximumDistanceSquared) {
                count++;
            }
        }
        return count;
    }

    private static Color averageColor(
            BufferedImage image,
            int y,
            int startX,
            int endX) {

        long red = 0;
        long green = 0;
        long blue = 0;
        for (int x = startX; x < endX; x++) {
            Color color = new Color(image.getRGB(x, y));
            red += color.getRed();
            green += color.getGreen();
            blue += color.getBlue();
        }

        int count = endX - startX;
        return new Color(
                (int) (red / count),
                (int) (green / count),
                (int) (blue / count));
    }

    private static double colorStandardDeviation(
            BufferedImage image,
            int y,
            int startX,
            int endX,
            Color average) {

        long squaredSum = 0;
        for (int x = startX; x < endX; x++) {
            Color color = new Color(image.getRGB(x, y));
            squaredSum += colorDistanceSquared(color, average);
        }
        return Math.sqrt(squaredSum / (double) (endX - startX));
    }

    private static double colorDistance(Color first, Color second) {
        return Math.sqrt(colorDistanceSquared(first, second));
    }

    private static int colorDistanceSquared(
            Color first,
            Color second) {

        int red = first.getRed() - second.getRed();
        int green = first.getGreen() - second.getGreen();
        int blue = first.getBlue() - second.getBlue();
        return red * red + green * green + blue * blue;
    }
}

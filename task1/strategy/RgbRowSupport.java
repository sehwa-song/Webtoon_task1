package task1.strategy;

import java.awt.Color;
import java.awt.image.BufferedImage;

/** 세 RowClassifier가 함께 사용하는 순수 RGB 계산 함수 모음입니다. */
final class RgbRowSupport {
    static final double NEUTRAL_FOREGROUND_RATIO = 0.02;
    static final double COLORED_FOREGROUND_RATIO = 0.005;

    private static final double EDGE_SAMPLE_RATIO = 0.05;
    private static final double BACKGROUND_COLOR_DISTANCE = 18.0;
    private static final double MAX_EDGE_COLOR_DISTANCE = 18.0;
    private static final double MAX_EDGE_STANDARD_DEVIATION = 8.0;
    private static final int MAX_NEUTRAL_CHANNEL_RANGE = 18;

    private RgbRowSupport() {
    }

    static int countWhiteOrBlackForegroundPixels(
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

    static int countEdgeBasedForegroundPixels(
            BufferedImage image,
            int y) {

        Color background = findExpectedEdgeBackgroundColor(image, y);
        if (background == null) {
            return image.getWidth();
        }
        return countPixelsDifferentFromBackground(image, y, background);
    }

    static Color findExpectedEdgeBackgroundColor(
            BufferedImage image,
            int y) {

        int width = image.getWidth();
        int edgeWidth = Math.max(
                1,
                (int) Math.round(width * EDGE_SAMPLE_RATIO));
        edgeWidth = Math.min(edgeWidth, Math.max(1, width / 3));

        Color left = averageColor(image, y, 0, edgeWidth);
        Color right = averageColor(
                image,
                y,
                width - edgeWidth,
                width);

        if (colorDistance(left, right) > MAX_EDGE_COLOR_DISTANCE) {
            return null;
        }

        double leftDeviation = colorStandardDeviation(
                image,
                y,
                0,
                edgeWidth,
                left);
        double rightDeviation = colorStandardDeviation(
                image,
                y,
                width - edgeWidth,
                width,
                right);

        if (leftDeviation > MAX_EDGE_STANDARD_DEVIATION
                || rightDeviation > MAX_EDGE_STANDARD_DEVIATION) {
            return null;
        }

        return new Color(
                (left.getRed() + right.getRed()) / 2,
                (left.getGreen() + right.getGreen()) / 2,
                (left.getBlue() + right.getBlue()) / 2);
    }

    static int countPixelsDifferentFromBackground(
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

    static boolean isNeutralColor(Color color) {
        int maximum = Math.max(
                color.getRed(),
                Math.max(color.getGreen(), color.getBlue()));
        int minimum = Math.min(
                color.getRed(),
                Math.min(color.getGreen(), color.getBlue()));
        return maximum - minimum <= MAX_NEUTRAL_CHANNEL_RANGE;
    }

    static double colorDistance(Color first, Color second) {
        return Math.sqrt(colorDistanceSquared(first, second));
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

    private static int colorDistanceSquared(
            Color first,
            Color second) {

        int red = first.getRed() - second.getRed();
        int green = first.getGreen() - second.getGreen();
        int blue = first.getBlue() - second.getBlue();
        return red * red + green * green + blue * blue;
    }
}

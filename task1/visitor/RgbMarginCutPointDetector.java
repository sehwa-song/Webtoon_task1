package task1.visitor;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** Task1LearningBaseline과 같은 RGB 여백 탐색 알고리즘을 이 패키지 안에 독립 포함합니다. */
final class RgbMarginCutPointDetector {
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
    private static final int CONTENT_ROW = 0;
    private static final int NEUTRAL_BLANK_ROW = 1;
    private static final int COLORED_BLANK_ROW = 2;

    List<Integer> findCutPoints(BufferedImage image) {
        List<Integer> cuts = new ArrayList<>();
        cuts.add(0);
        int blankStart = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            boolean blank = classifyBlankRow(image, y) != CONTENT_ROW;
            if (blank && blankStart == -1) {
                blankStart = y;
            } else if (!blank && blankStart != -1) {
                addCutPointForBlankZone(image, cuts, blankStart, y - 1);
                blankStart = -1;
            }
        }
        if (blankStart != -1) {
            addCutPointForBlankZone(
                    image, cuts, blankStart, image.getHeight() - 1);
        }
        cuts.add(image.getHeight());
        return cuts;
    }

    private static int classifyBlankRow(BufferedImage image, int y) {
        int neutralMaximum =
                (int) (image.getWidth() * NEUTRAL_FOREGROUND_RATIO);
        int coloredMaximum =
                (int) (image.getWidth() * COLORED_FOREGROUND_RATIO);
        if (countWhiteOrBlackForegroundPixels(image, y) <= neutralMaximum) {
            return NEUTRAL_BLANK_ROW;
        }
        Color expected = findExpectedEdgeBackgroundColor(image, y);
        if (expected == null) {
            return CONTENT_ROW;
        }
        int foreground = countPixelsDifferentFromBackground(image, y, expected);
        if (isNeutralColor(expected)) {
            return foreground <= neutralMaximum
                    ? NEUTRAL_BLANK_ROW : CONTENT_ROW;
        }
        return foreground <= coloredMaximum
                ? COLORED_BLANK_ROW : CONTENT_ROW;
    }

    private static int countForegroundPixels(BufferedImage image, int y) {
        return Math.min(
                countWhiteOrBlackForegroundPixels(image, y),
                countEdgeBasedForegroundPixels(image, y));
    }

    private static int countWhiteOrBlackForegroundPixels(
            BufferedImage image, int y) {
        int white = 0;
        int black = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            Color color = new Color(image.getRGB(x, y));
            if (color.getRed() >= 245 && color.getGreen() >= 245
                    && color.getBlue() >= 245) {
                white++;
            }
            if (color.getRed() <= 20 && color.getGreen() <= 20
                    && color.getBlue() <= 20) {
                black++;
            }
        }
        return image.getWidth() - Math.max(white, black);
    }

    private static int countEdgeBasedForegroundPixels(
            BufferedImage image, int y) {
        Color expected = findExpectedEdgeBackgroundColor(image, y);
        return expected == null
                ? image.getWidth()
                : countPixelsDifferentFromBackground(image, y, expected);
    }

    private static Color findExpectedEdgeBackgroundColor(
            BufferedImage image, int y) {
        int width = image.getWidth();
        int edgeWidth = Math.max(
                1, (int) Math.round(width * EDGE_SAMPLE_RATIO));
        edgeWidth = Math.min(edgeWidth, Math.max(1, width / 3));
        Color left = averageColor(image, y, 0, edgeWidth);
        Color right = averageColor(image, y, width - edgeWidth, width);
        if (colorDistance(left, right) > MAX_EDGE_COLOR_DISTANCE) {
            return null;
        }
        if (colorStandardDeviation(image, y, 0, edgeWidth, left)
                        > MAX_EDGE_STANDARD_DEVIATION
                || colorStandardDeviation(
                        image, y, width - edgeWidth, width, right)
                        > MAX_EDGE_STANDARD_DEVIATION) {
            return null;
        }
        return new Color(
                (left.getRed() + right.getRed()) / 2,
                (left.getGreen() + right.getGreen()) / 2,
                (left.getBlue() + right.getBlue()) / 2);
    }

    private static boolean isNeutralColor(Color color) {
        int maximum = Math.max(
                color.getRed(), Math.max(color.getGreen(), color.getBlue()));
        int minimum = Math.min(
                color.getRed(), Math.min(color.getGreen(), color.getBlue()));
        return maximum - minimum <= MAX_NEUTRAL_CHANNEL_RANGE;
    }

    private static int countPixelsDifferentFromBackground(
            BufferedImage image, int y, Color expected) {
        int count = 0;
        double limit = BACKGROUND_COLOR_DISTANCE * BACKGROUND_COLOR_DISTANCE;
        for (int x = 0; x < image.getWidth(); x++) {
            if (colorDistanceSquared(
                    new Color(image.getRGB(x, y)), expected) > limit) {
                count++;
            }
        }
        return count;
    }

    private static Color averageColor(
            BufferedImage image, int y, int startX, int endX) {
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
        long sum = 0;
        for (int x = startX; x < endX; x++) {
            sum += colorDistanceSquared(
                    new Color(image.getRGB(x, y)), average);
        }
        return Math.sqrt(sum / (double) (endX - startX));
    }

    private static double colorDistance(Color first, Color second) {
        return Math.sqrt(colorDistanceSquared(first, second));
    }

    private static int colorDistanceSquared(Color first, Color second) {
        int red = first.getRed() - second.getRed();
        int green = first.getGreen() - second.getGreen();
        int blue = first.getBlue() - second.getBlue();
        return red * red + green * green + blue * blue;
    }

    private static void addCutPointForBlankZone(
            BufferedImage image,
            List<Integer> cuts,
            int start,
            int end) {
        if (start < 0 || end < start
                || start == 0 || end == image.getHeight() - 1) {
            return;
        }
        boolean neutralQualified = false;
        boolean coloredQualified = false;
        int neutralRunLength = 0;
        int coloredRunStart = -1;

        for (int y = start; y <= end; y++) {
            int type = classifyBlankRow(image, y);
            if (type == NEUTRAL_BLANK_ROW) {
                neutralRunLength++;
                if (neutralRunLength >= MIN_NEUTRAL_BLANK_HEIGHT) {
                    neutralQualified = true;
                }
                if (coloredRunStart != -1) {
                    int coloredEnd = y - 1;
                    if (coloredEnd - coloredRunStart + 1
                                    >= MIN_COLORED_BLANK_HEIGHT
                            && isStableColoredBackground(
                                    image, coloredRunStart, coloredEnd)) {
                        coloredQualified = true;
                    }
                    coloredRunStart = -1;
                }
            } else if (type == COLORED_BLANK_ROW) {
                neutralRunLength = 0;
                if (coloredRunStart == -1) {
                    coloredRunStart = y;
                }
            }
        }

        if (coloredRunStart != -1
                && end - coloredRunStart + 1 >= MIN_COLORED_BLANK_HEIGHT
                && isStableColoredBackground(image, coloredRunStart, end)) {
            coloredQualified = true;
        }

        if (neutralQualified || coloredQualified) {
            int cutY = findSafestCutY(image, start, end);
            if (cutY != -1) {
                cuts.add(cutY);
            }
        }
    }

    private static boolean isStableColoredBackground(
            BufferedImage image, int start, int end) {
        Color reference = findExpectedEdgeBackgroundColor(
                image, start + (end - start) / 2);
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

    private static int findSafestCutY(
            BufferedImage image, int start, int end) {
        int radius = 5;
        int middle = start + (end - start) / 2;
        int maximumForeground = Math.max(
                1, (int) (image.getWidth() * COLORED_FOREGROUND_RATIO));
        int bestY = -1;
        long bestScore = Long.MAX_VALUE;
        for (int y = start + radius; y <= end - radius; y++) {
            long score = 0;
            boolean safe = true;
            for (int checkY = y - radius; checkY <= y + radius; checkY++) {
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
            if (score < bestScore
                    || (bestY != -1 && score == bestScore
                            && Math.abs(y - middle) < Math.abs(bestY - middle))) {
                bestY = y;
                bestScore = score;
            }
        }
        return bestY;
    }
}

package task1.decorator;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Task1LearningBaseline의 최신 RGB 컷 탐색 알고리즘을 이 패키지 안에 독립적으로 둡니다.
 * 다른 패턴 패키지나 Baseline 클래스를 호출하지 않습니다.
 */
final class AdaptiveRgbCutAlgorithm {
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

    private AdaptiveRgbCutAlgorithm() {
    }

    static List<Integer> findCutPoints(BufferedImage image) {
        Objects.requireNonNull(image, "image");
        List<Integer> points = new ArrayList<>();
        points.add(0);
        int blankStart = -1;

        for (int y = 0; y < image.getHeight(); y++) {
            boolean blank = classifyBlankRow(image, y) != CONTENT_ROW;
            if (blank && blankStart == -1) {
                blankStart = y;
            }
            if (!blank && blankStart != -1) {
                addCutForBlankZone(image, points, blankStart, y - 1);
                blankStart = -1;
            }
        }

        if (blankStart != -1) {
            addCutForBlankZone(image, points, blankStart, image.getHeight() - 1);
        }
        points.add(image.getHeight());
        return points;
    }

    private static int classifyBlankRow(BufferedImage image, int y) {
        int width = image.getWidth();
        int neutralMaximum = (int) (width * NEUTRAL_FOREGROUND_RATIO);
        int coloredMaximum = (int) (width * COLORED_FOREGROUND_RATIO);

        if (countWhiteOrBlackForeground(image, y) <= neutralMaximum) {
            return NEUTRAL_BLANK_ROW;
        }

        Color background = findEdgeBackground(image, y);
        if (background == null) {
            return CONTENT_ROW;
        }

        int foreground = countDifferentPixels(image, y, background);
        if (isNeutral(background)) {
            return foreground <= neutralMaximum ? NEUTRAL_BLANK_ROW : CONTENT_ROW;
        }
        return foreground <= coloredMaximum ? COLORED_BLANK_ROW : CONTENT_ROW;
    }

    private static int countForegroundPixels(BufferedImage image, int y) {
        int fixed = countWhiteOrBlackForeground(image, y);
        Color background = findEdgeBackground(image, y);
        int edge = background == null
                ? image.getWidth()
                : countDifferentPixels(image, y, background);
        return Math.min(fixed, edge);
    }

    private static int countWhiteOrBlackForeground(BufferedImage image, int y) {
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

    private static Color findEdgeBackground(BufferedImage image, int y) {
        int width = image.getWidth();
        int edgeWidth = Math.max(1, (int) Math.round(width * EDGE_SAMPLE_RATIO));
        edgeWidth = Math.min(edgeWidth, Math.max(1, width / 3));

        Color left = averageColor(image, y, 0, edgeWidth);
        Color right = averageColor(image, y, width - edgeWidth, width);
        if (colorDistance(left, right) > MAX_EDGE_COLOR_DISTANCE) {
            return null;
        }
        if (standardDeviation(image, y, 0, edgeWidth, left)
                    > MAX_EDGE_STANDARD_DEVIATION
                || standardDeviation(image, y, width - edgeWidth, width, right)
                    > MAX_EDGE_STANDARD_DEVIATION) {
            return null;
        }
        return new Color((left.getRed() + right.getRed()) / 2,
                (left.getGreen() + right.getGreen()) / 2,
                (left.getBlue() + right.getBlue()) / 2);
    }

    private static int countDifferentPixels(
            BufferedImage image, int y, Color background) {
        int count = 0;
        double maximum = BACKGROUND_COLOR_DISTANCE * BACKGROUND_COLOR_DISTANCE;
        for (int x = 0; x < image.getWidth(); x++) {
            if (colorDistanceSquared(new Color(image.getRGB(x, y)), background)
                    > maximum) {
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
        return new Color((int) (red / count), (int) (green / count),
                (int) (blue / count));
    }

    private static double standardDeviation(BufferedImage image, int y,
            int startX, int endX, Color average) {
        long sum = 0;
        for (int x = startX; x < endX; x++) {
            sum += colorDistanceSquared(new Color(image.getRGB(x, y)), average);
        }
        return Math.sqrt(sum / (double) (endX - startX));
    }

    private static boolean isNeutral(Color color) {
        int max = Math.max(color.getRed(), Math.max(color.getGreen(), color.getBlue()));
        int min = Math.min(color.getRed(), Math.min(color.getGreen(), color.getBlue()));
        return max - min <= MAX_NEUTRAL_CHANNEL_RANGE;
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

    private static void addCutForBlankZone(BufferedImage image,
            List<Integer> points, int start, int end) {
        if (start < 0 || end < start || start == 0
                || end == image.getHeight() - 1) {
            return;
        }

        boolean neutralQualified = false;
        boolean coloredQualified = false;
        int neutralLength = 0;
        int coloredStart = -1;

        for (int y = start; y <= end; y++) {
            int type = classifyBlankRow(image, y);
            if (type == NEUTRAL_BLANK_ROW) {
                neutralLength++;
                if (neutralLength >= MIN_NEUTRAL_BLANK_HEIGHT) {
                    neutralQualified = true;
                }
                if (coloredStart != -1) {
                    int coloredEnd = y - 1;
                    if (coloredEnd - coloredStart + 1 >= MIN_COLORED_BLANK_HEIGHT
                            && isStableColoredBackground(image, coloredStart, coloredEnd)) {
                        coloredQualified = true;
                    }
                    coloredStart = -1;
                }
            } else if (type == COLORED_BLANK_ROW) {
                neutralLength = 0;
                if (coloredStart == -1) {
                    coloredStart = y;
                }
            }
        }

        if (coloredStart != -1 && end - coloredStart + 1 >= MIN_COLORED_BLANK_HEIGHT
                && isStableColoredBackground(image, coloredStart, end)) {
            coloredQualified = true;
        }

        if (neutralQualified || coloredQualified) {
            int cutY = findSafestCutY(image, start, end);
            if (cutY != -1) {
                points.add(cutY);
            }
        }
    }

    private static boolean isStableColoredBackground(
            BufferedImage image, int start, int end) {
        Color reference = findEdgeBackground(image, start + (end - start) / 2);
        if (reference == null || isNeutral(reference)) {
            return false;
        }
        for (int y = start; y <= end; y++) {
            Color current = findEdgeBackground(image, y);
            if (current == null || isNeutral(current)
                    || colorDistance(current, reference) > MAX_COLORED_BACKGROUND_DRIFT) {
                return false;
            }
        }
        return true;
    }

    private static int findSafestCutY(BufferedImage image, int start, int end) {
        int radius = 5;
        int middle = start + (end - start) / 2;
        int maximum = Math.max(1,
                (int) (image.getWidth() * COLORED_FOREGROUND_RATIO));
        int bestY = -1;
        long bestScore = Long.MAX_VALUE;

        for (int y = start + radius; y <= end - radius; y++) {
            long score = 0;
            boolean safe = true;
            for (int checkY = y - radius; checkY <= y + radius; checkY++) {
                int foreground = countForegroundPixels(image, checkY);
                score += foreground;
                if (foreground > maximum) {
                    safe = false;
                    break;
                }
            }
            if (safe && (score < bestScore || (score == bestScore && bestY != -1
                    && Math.abs(y - middle) < Math.abs(bestY - middle)))) {
                bestY = y;
                bestScore = score;
            }
        }
        return bestY;
    }
}

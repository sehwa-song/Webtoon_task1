package task1.template;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Template Method의 컷 탐색 훅을 RGB 여백 알고리즘으로 구현한 클래스입니다.
 * 흰색/검은색뿐 아니라 좌우 배경색, 컬러 여백, 그라데이션과 말풍선도
 * 함께 고려합니다.
 */
public final class RgbMarginTemplateSplitter
        extends AbstractWebtoonSplitter {

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

    /** Template Method가 호출하는 구체 RGB 컷 탐색 훅입니다. */
    @Override
    protected List<Integer> detectCutPoints(BufferedImage image) {
        Objects.requireNonNull(image, "image");

        List<Integer> cutPoints = new ArrayList<>();
        cutPoints.add(0);
        int blankStart = -1;

        for (int y = 0; y < image.getHeight(); y++) {
            boolean blank = classifyBlankRow(image, y) != CONTENT_ROW;

            if (blank && blankStart == -1) {
                blankStart = y;
            }

            if (!blank && blankStart != -1) {
                addCutPointForBlankZone(
                        image, cutPoints, blankStart, y - 1);
                blankStart = -1;
            }
        }

        if (blankStart != -1) {
            addCutPointForBlankZone(
                    image,
                    cutPoints,
                    blankStart,
                    image.getHeight() - 1);
        }

        cutPoints.add(image.getHeight());
        return cutPoints;
    }

    /** 한 행을 그림/무채색 여백/컬러 여백으로 구분합니다. */
    private static int classifyBlankRow(BufferedImage image, int y) {
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

    /** 말풍선/글자를 피해 자를 때 사용할 현재 행의 전경 픽셀 수입니다. */
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

    /** 좌우 끝 5%의 색이 비슷하고 안정적이면 예상 배경색을 반환합니다. */
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

    /** 연속 여백 후보가 기준을 만족할 때만 안전한 cutY를 추가합니다. */
    private static void addCutPointForBlankZone(
            BufferedImage image,
            List<Integer> cutPoints,
            int start,
            int end) {

        if (start < 0 || end < start
                || start == 0
                || end == image.getHeight() - 1) {
            return;
        }

        boolean neutralQualified = false;
        boolean coloredQualified = false;
        int neutralRunLength = 0;
        int coloredRunStart = -1;

        for (int y = start; y <= end; y++) {
            int rowType = classifyBlankRow(image, y);

            if (rowType == NEUTRAL_BLANK_ROW) {
                neutralRunLength++;
                if (neutralRunLength >= MIN_NEUTRAL_BLANK_HEIGHT) {
                    neutralQualified = true;
                }

                if (coloredRunStart != -1) {
                    int coloredRunEnd = y - 1;
                    if (coloredRunEnd - coloredRunStart + 1
                                    >= MIN_COLORED_BLANK_HEIGHT
                            && isStableColoredBackground(
                                    image,
                                    coloredRunStart,
                                    coloredRunEnd)) {
                        coloredQualified = true;
                    }
                    coloredRunStart = -1;
                }
            } else if (rowType == COLORED_BLANK_ROW) {
                neutralRunLength = 0;
                if (coloredRunStart == -1) {
                    coloredRunStart = y;
                }
            }
        }

        if (coloredRunStart != -1
                && end - coloredRunStart + 1
                        >= MIN_COLORED_BLANK_HEIGHT
                && isStableColoredBackground(
                        image, coloredRunStart, end)) {
            coloredQualified = true;
        }

        if (!neutralQualified && !coloredQualified) {
            return;
        }

        int cutY = findSafestCutY(image, start, end);
        if (cutY != -1) {
            cutPoints.add(cutY);
        }
    }

    /** 컬러 여백이 세로 방향으로 거의 같은 색을 유지하는지 검사합니다. */
    private static boolean isStableColoredBackground(
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

    /** 여백 중앙 주변에서 11개 행이 모두 안전한 위치를 선택합니다. */
    private static int findSafestCutY(
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
            boolean closerToMiddle = bestY != -1
                    && score == bestScore
                    && Math.abs(y - middle) < Math.abs(bestY - middle);
            if (lowerScore || closerToMiddle) {
                bestY = y;
                bestScore = score;
            }
        }
        return bestY;
    }
}

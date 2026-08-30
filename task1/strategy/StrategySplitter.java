package task1.strategy;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Strategy 패턴의 Context입니다.
 * 컷 탐색 순서는 공통으로 유지하고, 행 판정만 RowClassifier에 위임합니다.
 */
public final class StrategySplitter {
    private static final int MIN_NEUTRAL_BLANK_HEIGHT = 50;
    private static final int MIN_COLORED_BLANK_HEIGHT = 100;
    private static final double MAX_COLORED_BACKGROUND_DRIFT = 10.0;

    private RowClassifier rowClassifier;

    public StrategySplitter(RowClassifier rowClassifier) {
        setStrategy(rowClassifier);
    }

    public List<Integer> findCutPoints(BufferedImage image) {
        Objects.requireNonNull(image, "image");

        List<Integer> cutPoints = new ArrayList<>();
        cutPoints.add(0);
        int blankStart = -1;

        for (int y = 0; y < image.getHeight(); y++) {
            boolean blank = rowClassifier.classify(image, y)
                    != RowType.CONTENT;

            if (blank && blankStart == -1) {
                blankStart = y;
            }

            if (!blank && blankStart != -1) {
                addCutPointForBlankZone(
                        image,
                        cutPoints,
                        blankStart,
                        y - 1);
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

    /** 실행 중에도 RowClassifier 전략을 교체할 수 있습니다. */
    public void setStrategy(RowClassifier rowClassifier) {
        this.rowClassifier = Objects.requireNonNull(
                rowClassifier,
                "rowClassifier");
    }

    public RowClassifier getStrategy() {
        return rowClassifier;
    }

    private void addCutPointForBlankZone(
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
            RowType rowType = rowClassifier.classify(image, y);

            if (rowType == RowType.NEUTRAL_BLANK) {
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
            } else if (rowType == RowType.COLORED_BLANK) {
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
                        image,
                        coloredRunStart,
                        end)) {
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

    private boolean isStableColoredBackground(
            BufferedImage image,
            int start,
            int end) {

        int middle = start + (end - start) / 2;
        Color reference = rowClassifier.findExpectedBackgroundColor(
                image,
                middle);

        if (reference == null || RgbRowSupport.isNeutralColor(reference)) {
            return false;
        }

        for (int y = start; y <= end; y++) {
            Color current = rowClassifier.findExpectedBackgroundColor(
                    image,
                    y);

            if (current == null
                    || RgbRowSupport.isNeutralColor(current)
                    || RgbRowSupport.colorDistance(current, reference)
                            > MAX_COLORED_BACKGROUND_DRIFT) {
                return false;
            }
        }
        return true;
    }

    private int findSafestCutY(
            BufferedImage image,
            int start,
            int end) {

        int radius = 5;
        int middle = start + (end - start) / 2;
        int maximumForeground = Math.max(
                1,
                (int) (
                        image.getWidth()
                        * RgbRowSupport.COLORED_FOREGROUND_RATIO));
        int bestY = -1;
        long bestScore = Long.MAX_VALUE;

        for (int y = start + radius; y <= end - radius; y++) {
            long score = 0;
            boolean safe = true;

            for (int checkY = y - radius;
                    checkY <= y + radius;
                    checkY++) {

                int foreground = rowClassifier.countForegroundPixels(
                        image,
                        checkY);
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

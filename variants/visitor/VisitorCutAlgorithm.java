package variants.visitor;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Visitor 실험군이 독립적으로 사용하는 RGB-margin 컷 검출 구현이다.
 * 다른 실험군의 구현에는 의존하지 않는다.
 */
final class VisitorCutAlgorithm {
    private static final int MIN_GUTTER_HEIGHT = 80;
    private static final int MIN_DISTANCE_BETWEEN_CUTS = 250;

    private static final double BACKGROUND_DISTANCE = 18.0;
    private static final double MARGIN_COLOR_DISTANCE = 18.0;
    private static final double MAX_MARGIN_STD_DEV = 8.0;
    private static final double MAX_ROW_STD_DEV = 18.0;
    private static final double STRONG_BACKGROUND_RATIO = 0.94;
    private static final double STRONG_MARGIN_RATIO = 0.90;

    private static final double WEAK_BACKGROUND_RATIO = 0.68;
    private static final double WEAK_MARGIN_RATIO = 0.75;
    private static final int MAX_STRONG_INTERRUPTION = 4;
    private static final int MAX_WEAK_EXPANSION = 80;
    private static final int MAX_OBSTRUCTION_HEIGHT = 180;

    private VisitorCutAlgorithm() {
    }

    static List<Integer> findCutPoints(BufferedImage image) {
        RowAnalysis rows = analyzeRows(image);
        boolean[] strongBlank = closeShortInterruptions(
                rows.strongBlank, MAX_STRONG_INTERRUPTION);

        List<SeparatorZone> zones = findSeparatorZones(strongBlank, rows.weakBlank);
        List<CutCandidate> candidates = new ArrayList<>();

        for (SeparatorZone zone : zones) {
            // 맨 위와 맨 아래의 바탕은 컷 사이 여백으로 사용하지 않는다.
            if (zone.coreStart == 0 || zone.coreEnd == image.getHeight() - 1) {
                continue;
            }

            int cutY = chooseSafeMiddle(zone, strongBlank, rows);
            candidates.add(new CutCandidate(cutY, zone.strongRowCount));
        }

        List<Integer> cutYs = new ArrayList<>();
        cutYs.add(0);
        for (CutCandidate candidate : keepBestSeparatedCandidates(candidates)) {
            cutYs.add(candidate.y);
        }
        cutYs.add(image.getHeight());
        return cutYs;
    }

    private static RowAnalysis analyzeRows(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int marginWidth = Math.max(12, width / 20);
        marginWidth = Math.min(marginWidth, Math.max(1, width / 3));

        RowAnalysis result = new RowAnalysis(height);
        int[] row = new int[width];
        ColorModeFinder modeFinder = new ColorModeFinder();
        double backgroundDistanceSquared = BACKGROUND_DISTANCE * BACKGROUND_DISTANCE;

        for (int y = 0; y < height; y++) {
            image.getRGB(0, y, width, 1, row, 0, width);

            RgbColor left = modeFinder.find(row, 0, marginWidth);
            RgbColor right = modeFinder.find(row, width - marginWidth, width);
            RgbColor background = RgbColor.average(left, right);

            double marginColorDistance = colorDistance(left, right);
            double leftStdDev = colorStdDev(row, 0, marginWidth);
            double rightStdDev = colorStdDev(row, width - marginWidth, width);

            int backgroundPixels = 0;
            int matchingMarginPixels = 0;
            long sumR = 0;
            long sumG = 0;
            long sumB = 0;
            long sumR2 = 0;
            long sumG2 = 0;
            long sumB2 = 0;

            for (int x = 0; x < width; x++) {
                int rgb = row[x];
                int r = (rgb >>> 16) & 0xff;
                int g = (rgb >>> 8) & 0xff;
                int b = rgb & 0xff;

                if (colorDistanceSquared(r, g, b, background)
                        <= backgroundDistanceSquared) {
                    backgroundPixels++;
                    if (x < marginWidth || x >= width - marginWidth) {
                        matchingMarginPixels++;
                    }
                }

                sumR += r;
                sumG += g;
                sumB += b;
                sumR2 += (long) r * r;
                sumG2 += (long) g * g;
                sumB2 += (long) b * b;
            }

            double backgroundRatio = backgroundPixels / (double) width;
            double marginRatio = matchingMarginPixels / (double) (marginWidth * 2);
            double rowStdDev = combinedStdDev(
                    width, sumR, sumG, sumB, sumR2, sumG2, sumB2);

            boolean cleanMargins = marginColorDistance <= MARGIN_COLOR_DISTANCE
                    && leftStdDev <= MAX_MARGIN_STD_DEV
                    && rightStdDev <= MAX_MARGIN_STD_DEV;

            result.backgroundRatio[y] = backgroundRatio;
            result.rowStdDev[y] = rowStdDev;
            result.strongBlank[y] = cleanMargins
                    && marginRatio >= STRONG_MARGIN_RATIO
                    && backgroundRatio >= STRONG_BACKGROUND_RATIO
                    && rowStdDev <= MAX_ROW_STD_DEV;

            result.weakBlank[y] = marginColorDistance <= MARGIN_COLOR_DISTANCE * 1.5
                    && leftStdDev <= MAX_MARGIN_STD_DEV * 1.75
                    && rightStdDev <= MAX_MARGIN_STD_DEV * 1.75
                    && marginRatio >= WEAK_MARGIN_RATIO
                    && backgroundRatio >= WEAK_BACKGROUND_RATIO;
        }
        return result;
    }

    private static List<SeparatorZone> findSeparatorZones(
            boolean[] strongBlank, boolean[] weakBlank) {
        List<SeparatorZone> zones = new ArrayList<>();
        int y = 0;

        while (y < strongBlank.length) {
            if (!strongBlank[y]) {
                y++;
                continue;
            }

            int start = y;
            while (y + 1 < strongBlank.length && strongBlank[y + 1]) {
                y++;
            }
            int end = y;

            if (end - start + 1 >= MIN_GUTTER_HEIGHT) {
                int expandedStart = start;
                int expandedEnd = end;

                int expanded = 0;
                while (expandedStart > 0
                        && expanded < MAX_WEAK_EXPANSION
                        && weakBlank[expandedStart - 1]) {
                    expandedStart--;
                    expanded++;
                }

                expanded = 0;
                while (expandedEnd + 1 < weakBlank.length
                        && expanded < MAX_WEAK_EXPANSION
                        && weakBlank[expandedEnd + 1]) {
                    expandedEnd++;
                    expanded++;
                }

                zones.add(new SeparatorZone(
                        expandedStart, expandedEnd, start, end, end - start + 1));
            }
            y++;
        }

        if (zones.size() < 2) {
            return zones;
        }

        List<SeparatorZone> merged = new ArrayList<>();
        SeparatorZone current = zones.get(0);

        for (int i = 1; i < zones.size(); i++) {
            SeparatorZone next = zones.get(i);
            int obstructionHeight = next.coreStart - current.coreEnd - 1;

            if (shouldMergeZones(current, next, weakBlank, obstructionHeight)) {
                current = current.merge(next);
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    private static boolean shouldMergeZones(
            SeparatorZone current,
            SeparatorZone next,
            boolean[] weakBlank,
            int obstructionHeight) {
        if (obstructionHeight <= 0) {
            return true;
        }
        if (obstructionHeight > MAX_OBSTRUCTION_HEIGHT) {
            return false;
        }

        int weakRows = 0;
        for (int y = current.coreEnd + 1; y < next.coreStart; y++) {
            if (weakBlank[y]) {
                weakRows++;
            }
        }
        return weakRows / (double) obstructionHeight >= 0.60;
    }

    private static int chooseSafeMiddle(
            SeparatorZone zone, boolean[] strongBlank, RowAnalysis rows) {
        int middle = (zone.start + zone.end) / 2;
        int bestY = -1;
        int bestDistance = Integer.MAX_VALUE;
        double bestSafety = Double.NEGATIVE_INFINITY;

        for (int y = zone.start; y <= zone.end; y++) {
            if (!strongBlank[y]) {
                continue;
            }

            int distance = Math.abs(y - middle);
            double safety = rows.backgroundRatio[y] - rows.rowStdDev[y] / 255.0;
            if (distance < bestDistance
                    || (distance == bestDistance && safety > bestSafety)) {
                bestY = y;
                bestDistance = distance;
                bestSafety = safety;
            }
        }
        return bestY >= 0 ? bestY : (zone.coreStart + zone.coreEnd) / 2;
    }

    private static List<CutCandidate> keepBestSeparatedCandidates(
            List<CutCandidate> candidates) {
        List<CutCandidate> result = new ArrayList<>();

        for (CutCandidate candidate : candidates) {
            if (result.isEmpty()) {
                result.add(candidate);
                continue;
            }

            CutCandidate previous = result.get(result.size() - 1);
            if (candidate.y - previous.y >= MIN_DISTANCE_BETWEEN_CUTS) {
                result.add(candidate);
                continue;
            }

            if (candidate.quality > previous.quality) {
                result.set(result.size() - 1, candidate);
            }
        }
        return result;
    }

    private static boolean[] closeShortInterruptions(boolean[] source, int maxGap) {
        boolean[] result = source.clone();
        int y = 0;

        while (y < source.length) {
            if (source[y]) {
                y++;
                continue;
            }

            int start = y;
            while (y < source.length && !source[y]) {
                y++;
            }
            int endExclusive = y;

            boolean boundedByBlank = start > 0
                    && endExclusive < source.length
                    && source[start - 1]
                    && source[endExclusive];
            if (boundedByBlank && endExclusive - start <= maxGap) {
                Arrays.fill(result, start, endExclusive, true);
            }
        }
        return result;
    }

    private static double colorDistance(RgbColor a, RgbColor b) {
        return Math.sqrt(colorDistanceSquared(a.r, a.g, a.b, b));
    }

    private static double colorDistanceSquared(int r, int g, int b, RgbColor other) {
        int dr = r - other.r;
        int dg = g - other.g;
        int db = b - other.b;
        return (double) dr * dr + (double) dg * dg + (double) db * db;
    }

    private static double colorStdDev(int[] pixels, int from, int to) {
        int count = to - from;
        long sumR = 0;
        long sumG = 0;
        long sumB = 0;
        long sumR2 = 0;
        long sumG2 = 0;
        long sumB2 = 0;

        for (int i = from; i < to; i++) {
            int rgb = pixels[i];
            int r = (rgb >>> 16) & 0xff;
            int g = (rgb >>> 8) & 0xff;
            int b = rgb & 0xff;
            sumR += r;
            sumG += g;
            sumB += b;
            sumR2 += (long) r * r;
            sumG2 += (long) g * g;
            sumB2 += (long) b * b;
        }
        return combinedStdDev(count, sumR, sumG, sumB, sumR2, sumG2, sumB2);
    }

    private static double combinedStdDev(
            int count,
            long sumR,
            long sumG,
            long sumB,
            long sumR2,
            long sumG2,
            long sumB2) {
        double meanR = sumR / (double) count;
        double meanG = sumG / (double) count;
        double meanB = sumB / (double) count;
        double varianceR = Math.max(0.0, sumR2 / (double) count - meanR * meanR);
        double varianceG = Math.max(0.0, sumG2 / (double) count - meanG * meanG);
        double varianceB = Math.max(0.0, sumB2 / (double) count - meanB * meanB);
        return Math.sqrt((varianceR + varianceG + varianceB) / 3.0);
    }

    private static final class RowAnalysis {
        final boolean[] strongBlank;
        final boolean[] weakBlank;
        final double[] backgroundRatio;
        final double[] rowStdDev;

        RowAnalysis(int height) {
            strongBlank = new boolean[height];
            weakBlank = new boolean[height];
            backgroundRatio = new double[height];
            rowStdDev = new double[height];
        }
    }

    private static final class SeparatorZone {
        final int start;
        final int end;
        final int coreStart;
        final int coreEnd;
        final int strongRowCount;

        SeparatorZone(int start, int end, int coreStart, int coreEnd, int strongRowCount) {
            this.start = start;
            this.end = end;
            this.coreStart = coreStart;
            this.coreEnd = coreEnd;
            this.strongRowCount = strongRowCount;
        }

        SeparatorZone merge(SeparatorZone other) {
            return new SeparatorZone(
                    start,
                    other.end,
                    coreStart,
                    other.coreEnd,
                    strongRowCount + other.strongRowCount);
        }
    }

    private static final class CutCandidate {
        final int y;
        final int quality;

        CutCandidate(int y, int quality) {
            this.y = y;
            this.quality = quality;
        }
    }

    private static final class RgbColor {
        final int r;
        final int g;
        final int b;

        RgbColor(int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        static RgbColor average(RgbColor a, RgbColor b) {
            return new RgbColor(
                    (a.r + b.r) / 2,
                    (a.g + b.g) / 2,
                    (a.b + b.b) / 2);
        }
    }

    private static final class ColorModeFinder {
        private static final int COLOR_BIN_SHIFT = 4;
        private static final int COLOR_BIN_COUNT = 1 << (3 * (8 - COLOR_BIN_SHIFT));

        private final int[] counts = new int[COLOR_BIN_COUNT];
        private final int[] sumR = new int[COLOR_BIN_COUNT];
        private final int[] sumG = new int[COLOR_BIN_COUNT];
        private final int[] sumB = new int[COLOR_BIN_COUNT];
        private final int[] stamps = new int[COLOR_BIN_COUNT];
        private int generation = 1;

        RgbColor find(int[] pixels, int from, int to) {
            if (generation == Integer.MAX_VALUE) {
                Arrays.fill(stamps, 0);
                generation = 1;
            } else {
                generation++;
            }

            int bestBin = 0;
            int bestCount = 0;

            for (int i = from; i < to; i++) {
                int rgb = pixels[i];
                int r = (rgb >>> 16) & 0xff;
                int g = (rgb >>> 8) & 0xff;
                int b = rgb & 0xff;
                int bin = ((r >>> COLOR_BIN_SHIFT) << (2 * (8 - COLOR_BIN_SHIFT)))
                        | ((g >>> COLOR_BIN_SHIFT) << (8 - COLOR_BIN_SHIFT))
                        | (b >>> COLOR_BIN_SHIFT);

                if (stamps[bin] != generation) {
                    stamps[bin] = generation;
                    counts[bin] = 0;
                    sumR[bin] = 0;
                    sumG[bin] = 0;
                    sumB[bin] = 0;
                }

                counts[bin]++;
                sumR[bin] += r;
                sumG[bin] += g;
                sumB[bin] += b;

                if (counts[bin] > bestCount) {
                    bestBin = bin;
                    bestCount = counts[bin];
                }
            }

            return new RgbColor(
                    sumR[bestBin] / bestCount,
                    sumG[bestBin] / bestCount,
                    sumB[bestBin] / bestCount);
        }
    }
}

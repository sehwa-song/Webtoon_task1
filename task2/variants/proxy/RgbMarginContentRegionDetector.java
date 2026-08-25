package task2.variants.proxy;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Independent pure-Java Task2 detector.
 *
 * <p>It owns the complete RGB-margin algorithm. It does not import or call the
 * baseline, Strategy, Visitor, Factory, or any other variant.</p>
 */
public final class RgbMarginContentRegionDetector
        implements ContentRegionDetector {
    private static final int BASE_WIDTH = 720;
    private static final int MIN_GUTTER_HEIGHT = 48;
    private static final int MIN_DISTANCE_BETWEEN_GAPS = 250;
    private static final int MAX_STRONG_INTERRUPTION = 4;
    private static final int MAX_SAFE_INTERRUPTION = 2;
    private static final int MAX_WEAK_EXPANSION = 80;
    private static final int MAX_OBSTRUCTION_HEIGHT = 180;
    private static final int MIN_SAFE_BLANK_RUN = 20;
    private static final int CONTENT_PROTECTION_RADIUS = 3;
    private static final int BOUNDARY_PADDING = 4;
    private static final int CONTENT_ANCHOR_WINDOW = 80;
    private static final int MIN_EDGE_BLANK = 20;
    private static final int MAX_BRIDGE_GUARD_HEIGHT = 80;
    private static final int BRIDGE_SCAN_MARGIN = 8;
    private static final int MAX_BOUNDARY_FOREGROUND_EXTENSION = 24;

    private static final double BACKGROUND_DISTANCE = 18.0;
    private static final double FOREGROUND_BRIDGE_DISTANCE = 32.0;
    private static final double MARGIN_COLOR_DISTANCE = 18.0;
    private static final double MAX_MARGIN_STD_DEV = 8.0;
    private static final double MAX_ROW_STD_DEV = 18.0;
    private static final double STRONG_BACKGROUND_RATIO = 0.94;
    private static final double STRONG_MARGIN_RATIO = 0.90;
    private static final double WEAK_BACKGROUND_RATIO = 0.68;
    private static final double WEAK_MARGIN_RATIO = 0.75;

    /** Ordinary stateless detector owned entirely by this variant. */
    public RgbMarginContentRegionDetector() {
    }

    @Override
    public List<ContentRegion> findContentRegions(BufferedImage image) {
        Objects.requireNonNull(image, "image");
        if (image.getWidth() <= 0 || image.getHeight() <= 0) {
            throw new IllegalArgumentException("Image dimensions must be positive");
        }

        double scale = scaleFor(image.getWidth());
        RowAnalysis rows = analyzeRows(image);
        boolean[] protectedContent = dilate(
                rows.protectedContent,
                scaled(CONTENT_PROTECTION_RADIUS, scale));

        boolean[] safeBlank = new boolean[image.getHeight()];
        for (int y = 0; y < safeBlank.length; y++) {
            safeBlank[y] = rows.weakBlank[y] && !protectedContent[y];
        }
        safeBlank = closeSafeInterruptions(
                safeBlank,
                protectedContent,
                scaled(MAX_SAFE_INTERRUPTION, scale));

        if (isEntirelyBlank(safeBlank, protectedContent)) {
            return Collections.emptyList();
        }

        boolean[] zoneBlank = closeShortInterruptions(
                rows.strongBlank,
                scaled(MAX_STRONG_INTERRUPTION, scale));
        List<SeparatorZone> zones = findSeparatorZones(
                zoneBlank,
                rows.weakBlank,
                scaled(MIN_GUTTER_HEIGHT, scale),
                scaled(MAX_WEAK_EXPANSION, scale),
                scaled(MAX_OBSTRUCTION_HEIGHT, scale));

        int topTrim = findTopTrim(safeBlank, rows, scale);
        int bottomTrim = findBottomTrim(safeBlank, rows, scale);
        if (topTrim >= bottomTrim) {
            return Collections.emptyList();
        }

        List<RemovalZone> removals = new ArrayList<RemovalZone>();
        for (SeparatorZone zone : zones) {
            if (zone.coreStart == 0 || zone.coreEnd == image.getHeight() - 1) {
                continue;
            }
            if (!hasContentAnchor(rows, zone.start - 1, -1, scale)
                    || !hasContentAnchor(rows, zone.end + 1, 1, scale)) {
                continue;
            }

            RemovalZone removal = chooseSafeRemoval(zone, safeBlank, scale);
            if (removal != null) {
                removal = protectForegroundAtBoundaries(
                        image, rows, removal, scale);
                if (removal != null
                        && !hasForegroundBridge(image, rows, removal, scale)) {
                    removals.add(removal);
                }
            }
        }

        removals = keepBestSeparatedRemovals(
                removals,
                scaled(MIN_DISTANCE_BETWEEN_GAPS, scale));

        List<ContentRegion> regions = complement(
                topTrim,
                bottomTrim,
                removals);
        return Collections.unmodifiableList(regions);
    }

    @Override
    public String name() {
        return "rgb-margin-content-region-detector";
    }

    private static List<ContentRegion> complement(
            int top,
            int bottom,
            List<RemovalZone> removals) {
        List<ContentRegion> regions = new ArrayList<ContentRegion>();
        int cursor = top;

        for (RemovalZone removal : removals) {
            int gapStart = Math.max(top, removal.start);
            int gapEnd = Math.min(bottom, removal.endExclusive);
            if (gapEnd <= cursor) {
                continue;
            }
            if (gapStart > cursor) {
                regions.add(new ContentRegion(cursor, gapStart));
            }
            cursor = gapEnd;
        }
        if (cursor < bottom) {
            regions.add(new ContentRegion(cursor, bottom));
        }
        return regions;
    }

    private static RowAnalysis analyzeRows(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int marginWidth = Math.max(12, width / 20);
        marginWidth = Math.min(marginWidth, Math.max(1, width / 3));

        RowAnalysis result = new RowAnalysis(height);
        int[] row = new int[width];
        ColorModeFinder modeFinder = new ColorModeFinder();
        double backgroundDistanceSquared =
                BACKGROUND_DISTANCE * BACKGROUND_DISTANCE;

        for (int y = 0; y < height; y++) {
            image.getRGB(0, y, width, 1, row, 0, width);

            RgbColor left = modeFinder.find(row, 0, marginWidth);
            RgbColor right = modeFinder.find(
                    row, width - marginWidth, width);
            RgbColor background = RgbColor.average(left, right);
            result.backgroundR[y] = background.r;
            result.backgroundG[y] = background.g;
            result.backgroundB[y] = background.b;

            double marginColorDistance = colorDistance(left, right);
            double leftStdDev = colorStdDev(row, 0, marginWidth);
            double rightStdDev = colorStdDev(
                    row, width - marginWidth, width);

            int backgroundPixels = 0;
            int matchingMarginPixels = 0;
            long sumR = 0L;
            long sumG = 0L;
            long sumB = 0L;
            long sumR2 = 0L;
            long sumG2 = 0L;
            long sumB2 = 0L;

            for (int x = 0; x < width; x++) {
                int rgb = compositeOnWhite(row[x]);
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
            double marginRatio = matchingMarginPixels
                    / (double) (marginWidth * 2);
            double rowStdDev = combinedStdDev(
                    width,
                    sumR,
                    sumG,
                    sumB,
                    sumR2,
                    sumG2,
                    sumB2);
            boolean cleanMargins = marginColorDistance <= MARGIN_COLOR_DISTANCE
                    && leftStdDev <= MAX_MARGIN_STD_DEV
                    && rightStdDev <= MAX_MARGIN_STD_DEV;

            result.backgroundRatio[y] = backgroundRatio;
            result.rowStdDev[y] = rowStdDev;
            result.strongBlank[y] = cleanMargins
                    && marginRatio >= STRONG_MARGIN_RATIO
                    && backgroundRatio >= STRONG_BACKGROUND_RATIO
                    && rowStdDev <= MAX_ROW_STD_DEV;
            result.weakBlank[y] = marginColorDistance
                    <= MARGIN_COLOR_DISTANCE * 1.5
                    && leftStdDev <= MAX_MARGIN_STD_DEV * 1.75
                    && rightStdDev <= MAX_MARGIN_STD_DEV * 1.75
                    && marginRatio >= WEAK_MARGIN_RATIO
                    && backgroundRatio >= WEAK_BACKGROUND_RATIO;
            result.protectedContent[y] = backgroundRatio < 0.85
                    || rowStdDev > 28.0
                    || ((1.0 - backgroundRatio) >= 0.015
                    && rowStdDev > 18.0)
                    || marginColorDistance > MARGIN_COLOR_DISTANCE * 1.5;
        }
        return result;
    }

    private static List<SeparatorZone> findSeparatorZones(
            boolean[] strongBlank,
            boolean[] weakBlank,
            int minGutterHeight,
            int maxWeakExpansion,
            int maxObstructionHeight) {
        List<SeparatorZone> zones = new ArrayList<SeparatorZone>();
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

            if (end - start + 1 >= minGutterHeight) {
                int expandedStart = start;
                int expandedEnd = end;
                int expanded = 0;
                while (expandedStart > 0
                        && expanded < maxWeakExpansion
                        && weakBlank[expandedStart - 1]) {
                    expandedStart--;
                    expanded++;
                }
                expanded = 0;
                while (expandedEnd + 1 < weakBlank.length
                        && expanded < maxWeakExpansion
                        && weakBlank[expandedEnd + 1]) {
                    expandedEnd++;
                    expanded++;
                }
                zones.add(new SeparatorZone(
                        expandedStart,
                        expandedEnd,
                        start,
                        end,
                        end - start + 1));
            }
            y++;
        }

        if (zones.size() < 2) {
            return zones;
        }

        List<SeparatorZone> merged = new ArrayList<SeparatorZone>();
        SeparatorZone current = zones.get(0);
        for (int index = 1; index < zones.size(); index++) {
            SeparatorZone next = zones.get(index);
            int obstructionHeight = next.coreStart - current.coreEnd - 1;
            if (shouldMergeZones(
                    current,
                    next,
                    weakBlank,
                    obstructionHeight,
                    maxObstructionHeight)) {
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
            int obstructionHeight,
            int maxObstructionHeight) {
        if (obstructionHeight <= 0) {
            return true;
        }
        if (obstructionHeight > maxObstructionHeight) {
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

    private static RemovalZone chooseSafeRemoval(
            SeparatorZone zone,
            boolean[] safeBlank,
            double scale) {
        int minRun = scaled(MIN_SAFE_BLANK_RUN, scale);
        int padding = scaled(BOUNDARY_PADDING, scale);
        int middle = (zone.start + zone.end) / 2;
        RemovalZone best = null;
        int y = zone.start;

        while (y <= zone.end) {
            if (!safeBlank[y]) {
                y++;
                continue;
            }
            int rawStart = y;
            while (y + 1 <= zone.end && safeBlank[y + 1]) {
                y++;
            }
            int rawEndExclusive = y + 1;
            int rawLength = rawEndExclusive - rawStart;
            int start = rawStart + padding;
            int endExclusive = rawEndExclusive - padding;

            if (rawLength >= minRun && start < endExclusive) {
                int center = (start + endExclusive) / 2;
                int quality = rawLength + zone.strongRowCount;
                RemovalZone candidate = new RemovalZone(
                        start,
                        endExclusive,
                        quality,
                        Math.abs(center - middle));
                if (best == null
                        || candidate.height() > best.height()
                        || (candidate.height() == best.height()
                        && candidate.middleDistance < best.middleDistance)) {
                    best = candidate;
                }
            }
            y++;
        }
        return best;
    }

    /** Reject a short candidate crossed by a connected thin foreground line. */
    private static boolean hasForegroundBridge(
            BufferedImage image,
            RowAnalysis rows,
            RemovalZone removal,
            double scale) {
        if (removal.height() > scaled(MAX_BRIDGE_GUARD_HEIGHT, scale)) {
            return false;
        }

        int margin = scaled(BRIDGE_SCAN_MARGIN, scale);
        int top = Math.max(0, removal.start - margin);
        int bottom = Math.min(image.getHeight(), removal.endExclusive + margin);
        int requiredTop = removal.start - top;
        int requiredBottom = removal.endExclusive - 1 - top;

        for (ForegroundComponent component
                : findForegroundComponents(image, rows, top, bottom)) {
            if (component.minY <= requiredTop
                    && component.maxY >= requiredBottom) {
                return true;
            }
        }
        return false;
    }

    private static RemovalZone protectForegroundAtBoundaries(
            BufferedImage image,
            RowAnalysis rows,
            RemovalZone removal,
            double scale) {
        int scanMargin = scaled(BRIDGE_SCAN_MARGIN, scale);
        int extension = scaled(MAX_BOUNDARY_FOREGROUND_EXTENSION, scale);
        int padding = scaled(BOUNDARY_PADDING, scale);
        int start = removal.start;
        int endExclusive = removal.endExclusive;

        int top = Math.max(0, start - scanMargin);
        int bottom = Math.min(image.getHeight(), start + extension + 1);
        int boundary = start - top;
        for (ForegroundComponent component
                : findForegroundComponents(image, rows, top, bottom)) {
            if (component.minY < boundary && component.maxY >= boundary) {
                start = Math.max(
                        start,
                        top + component.maxY + 1 + padding);
            }
        }

        top = Math.max(0, endExclusive - extension);
        bottom = Math.min(image.getHeight(), endExclusive + scanMargin);
        boundary = endExclusive - top;
        for (ForegroundComponent component
                : findForegroundComponents(image, rows, top, bottom)) {
            if (component.minY < boundary && component.maxY >= boundary) {
                endExclusive = Math.min(
                        endExclusive,
                        top + component.minY - padding);
            }
        }

        if (start >= endExclusive) {
            return null;
        }
        return new RemovalZone(
                start,
                endExclusive,
                removal.quality,
                removal.middleDistance);
    }

    private static List<ForegroundComponent> findForegroundComponents(
            BufferedImage image,
            RowAnalysis rows,
            int top,
            int bottom) {
        int width = image.getWidth();
        int bandHeight = bottom - top;
        boolean[] foreground = new boolean[width * bandHeight];
        int[] rowPixels = new int[width];
        double thresholdSquared =
                FOREGROUND_BRIDGE_DISTANCE * FOREGROUND_BRIDGE_DISTANCE;

        for (int localY = 0; localY < bandHeight; localY++) {
            int y = top + localY;
            image.getRGB(0, y, width, 1, rowPixels, 0, width);
            RgbColor background = new RgbColor(
                    rows.backgroundR[y], rows.backgroundG[y], rows.backgroundB[y]);
            int rowOffset = localY * width;
            for (int x = 0; x < width; x++) {
                int rgb = compositeOnWhite(rowPixels[x]);
                int r = (rgb >>> 16) & 0xff;
                int g = (rgb >>> 8) & 0xff;
                int b = rgb & 0xff;
                foreground[rowOffset + x] =
                        colorDistanceSquared(r, g, b, background) > thresholdSquared;
            }
        }

        List<ForegroundComponent> components =
                new ArrayList<ForegroundComponent>();
        boolean[] visited = new boolean[foreground.length];
        int[] queue = new int[foreground.length];
        for (int index = 0; index < foreground.length; index++) {
            if (!foreground[index] || visited[index]) {
                continue;
            }
            int head = 0;
            int tail = 0;
            queue[tail++] = index;
            visited[index] = true;
            int minY = index / width;
            int maxY = minY;

            while (head < tail) {
                int current = queue[head++];
                int currentY = current / width;
                int currentX = current - currentY * width;
                minY = Math.min(minY, currentY);
                maxY = Math.max(maxY, currentY);
                for (int dy = -1; dy <= 1; dy++) {
                    int nextY = currentY + dy;
                    if (nextY < 0 || nextY >= bandHeight) {
                        continue;
                    }
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) {
                            continue;
                        }
                        int nextX = currentX + dx;
                        if (nextX < 0 || nextX >= width) {
                            continue;
                        }
                        int next = nextY * width + nextX;
                        if (foreground[next] && !visited[next]) {
                            visited[next] = true;
                            queue[tail++] = next;
                        }
                    }
                }
            }
            components.add(new ForegroundComponent(minY, maxY));
        }
        return components;
    }

    private static boolean hasContentAnchor(
            RowAnalysis rows,
            int from,
            int direction,
            double scale) {
        int window = scaled(CONTENT_ANCHOR_WINDOW, scale);
        int evidence = 0;
        int checked = 0;
        int y = from;
        while (y >= 0 && y < rows.strongBlank.length && checked < window) {
            if (rows.protectedContent[y] || !rows.weakBlank[y]) {
                evidence++;
            }
            checked++;
            y += direction;
        }
        return checked > 0 && evidence / (double) checked >= 0.20;
    }

    private static int findTopTrim(
            boolean[] safeBlank,
            RowAnalysis rows,
            double scale) {
        int y = 0;
        while (y < safeBlank.length && safeBlank[y]) {
            y++;
        }
        int minimum = scaled(MIN_EDGE_BLANK, scale);
        if (y < minimum
                || y == safeBlank.length
                || !hasContentAnchor(rows, y, 1, scale)) {
            return 0;
        }
        if (y > safeBlank.length * 0.40
                && backgroundColorRange(rows, 0, y) > 18.0) {
            return 0;
        }
        return Math.max(0, y - scaled(BOUNDARY_PADDING, scale));
    }

    private static int findBottomTrim(
            boolean[] safeBlank,
            RowAnalysis rows,
            double scale) {
        int y = safeBlank.length - 1;
        while (y >= 0 && safeBlank[y]) {
            y--;
        }
        int runLength = safeBlank.length - 1 - y;
        int minimum = scaled(MIN_EDGE_BLANK, scale);
        if (runLength < minimum
                || y < 0
                || !hasContentAnchor(rows, y, -1, scale)) {
            return safeBlank.length;
        }
        if (runLength > safeBlank.length * 0.40
                && backgroundColorRange(
                rows, y + 1, safeBlank.length) > 18.0) {
            return safeBlank.length;
        }
        return Math.min(
                safeBlank.length,
                y + 1 + scaled(BOUNDARY_PADDING, scale));
    }

    private static List<RemovalZone> keepBestSeparatedRemovals(
            List<RemovalZone> candidates,
            int minimumDistance) {
        if (candidates.isEmpty()) {
            return candidates;
        }
        Collections.sort(
                candidates,
                Comparator.comparingInt(zone -> zone.start));
        List<RemovalZone> result = new ArrayList<RemovalZone>();

        for (RemovalZone candidate : candidates) {
            if (result.isEmpty()) {
                result.add(candidate);
                continue;
            }
            RemovalZone previous = result.get(result.size() - 1);
            int centerDistance = candidate.center() - previous.center();
            if (centerDistance >= minimumDistance
                    && candidate.start >= previous.endExclusive) {
                result.add(candidate);
            } else if (candidate.quality > previous.quality) {
                result.set(result.size() - 1, candidate);
            }
        }
        return result;
    }

    private static boolean isEntirelyBlank(
            boolean[] safeBlank,
            boolean[] protectedContent) {
        int safeRows = 0;
        int protectedRows = 0;
        for (int y = 0; y < safeBlank.length; y++) {
            if (safeBlank[y]) {
                safeRows++;
            }
            if (protectedContent[y]) {
                protectedRows++;
            }
        }
        return safeRows / (double) safeBlank.length >= 0.98
                && protectedRows == 0;
    }

    private static boolean[] dilate(boolean[] source, int radius) {
        boolean[] result = source.clone();
        for (int y = 0; y < source.length; y++) {
            if (!source[y]) {
                continue;
            }
            int start = Math.max(0, y - radius);
            int end = Math.min(source.length - 1, y + radius);
            Arrays.fill(result, start, end + 1, true);
        }
        return result;
    }

    private static boolean[] closeShortInterruptions(
            boolean[] source,
            int maxGap) {
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
            if (start > 0
                    && endExclusive < source.length
                    && source[start - 1]
                    && source[endExclusive]
                    && endExclusive - start <= maxGap) {
                Arrays.fill(result, start, endExclusive, true);
            }
        }
        return result;
    }

    private static boolean[] closeSafeInterruptions(
            boolean[] source,
            boolean[] protectedContent,
            int maxGap) {
        boolean[] result = source.clone();
        int y = 0;
        while (y < source.length) {
            if (source[y]) {
                y++;
                continue;
            }
            int start = y;
            boolean protectedInside = false;
            while (y < source.length && !source[y]) {
                protectedInside |= protectedContent[y];
                y++;
            }
            int endExclusive = y;
            if (!protectedInside
                    && start > 0
                    && endExclusive < source.length
                    && source[start - 1]
                    && source[endExclusive]
                    && endExclusive - start <= maxGap) {
                Arrays.fill(result, start, endExclusive, true);
            }
        }
        return result;
    }

    private static double backgroundColorRange(
            RowAnalysis rows,
            int from,
            int to) {
        if (to <= from) {
            return 0.0;
        }
        int minR = 255;
        int minG = 255;
        int minB = 255;
        int maxR = 0;
        int maxG = 0;
        int maxB = 0;
        for (int y = from; y < to; y++) {
            minR = Math.min(minR, rows.backgroundR[y]);
            minG = Math.min(minG, rows.backgroundG[y]);
            minB = Math.min(minB, rows.backgroundB[y]);
            maxR = Math.max(maxR, rows.backgroundR[y]);
            maxG = Math.max(maxG, rows.backgroundG[y]);
            maxB = Math.max(maxB, rows.backgroundB[y]);
        }
        int dr = maxR - minR;
        int dg = maxG - minG;
        int db = maxB - minB;
        return Math.sqrt((double) dr * dr + (double) dg * dg + (double) db * db);
    }

    private static double scaleFor(int width) {
        return Math.max(0.5, Math.min(2.0, width / (double) BASE_WIDTH));
    }

    private static int scaled(int value, double scale) {
        return Math.max(1, (int) Math.round(value * scale));
    }

    private static int compositeOnWhite(int argb) {
        int alpha = (argb >>> 24) & 0xff;
        if (alpha == 255) {
            return argb;
        }
        int r = (argb >>> 16) & 0xff;
        int g = (argb >>> 8) & 0xff;
        int b = argb & 0xff;
        r = (r * alpha + 255 * (255 - alpha)) / 255;
        g = (g * alpha + 255 * (255 - alpha)) / 255;
        b = (b * alpha + 255 * (255 - alpha)) / 255;
        return (0xff << 24) | (r << 16) | (g << 8) | b;
    }

    private static double colorDistance(RgbColor first, RgbColor second) {
        return Math.sqrt(colorDistanceSquared(
                first.r,
                first.g,
                first.b,
                second));
    }

    private static double colorDistanceSquared(
            int r,
            int g,
            int b,
            RgbColor other) {
        int dr = r - other.r;
        int dg = g - other.g;
        int db = b - other.b;
        return (double) dr * dr + (double) dg * dg + (double) db * db;
    }

    private static double colorStdDev(
            int[] pixels,
            int from,
            int to) {
        int count = to - from;
        long sumR = 0L;
        long sumG = 0L;
        long sumB = 0L;
        long sumR2 = 0L;
        long sumG2 = 0L;
        long sumB2 = 0L;
        for (int index = from; index < to; index++) {
            int rgb = compositeOnWhite(pixels[index]);
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
        return combinedStdDev(
                count,
                sumR,
                sumG,
                sumB,
                sumR2,
                sumG2,
                sumB2);
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
        double varianceR = Math.max(
                0.0,
                sumR2 / (double) count - meanR * meanR);
        double varianceG = Math.max(
                0.0,
                sumG2 / (double) count - meanG * meanG);
        double varianceB = Math.max(
                0.0,
                sumB2 / (double) count - meanB * meanB);
        return Math.sqrt((varianceR + varianceG + varianceB) / 3.0);
    }

    private static final class RowAnalysis {
        final boolean[] strongBlank;
        final boolean[] weakBlank;
        final boolean[] protectedContent;
        final double[] backgroundRatio;
        final double[] rowStdDev;
        final int[] backgroundR;
        final int[] backgroundG;
        final int[] backgroundB;

        RowAnalysis(int height) {
            strongBlank = new boolean[height];
            weakBlank = new boolean[height];
            protectedContent = new boolean[height];
            backgroundRatio = new double[height];
            rowStdDev = new double[height];
            backgroundR = new int[height];
            backgroundG = new int[height];
            backgroundB = new int[height];
        }
    }

    private static final class SeparatorZone {
        final int start;
        final int end;
        final int coreStart;
        final int coreEnd;
        final int strongRowCount;

        SeparatorZone(
                int start,
                int end,
                int coreStart,
                int coreEnd,
                int strongRowCount) {
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

    private static final class RemovalZone {
        final int start;
        final int endExclusive;
        final int quality;
        final int middleDistance;

        RemovalZone(
                int start,
                int endExclusive,
                int quality,
                int middleDistance) {
            this.start = start;
            this.endExclusive = endExclusive;
            this.quality = quality;
            this.middleDistance = middleDistance;
        }

        int height() {
            return endExclusive - start;
        }

        int center() {
            return (start + endExclusive) / 2;
        }
    }

    private static final class ForegroundComponent {
        final int minY;
        final int maxY;

        ForegroundComponent(int minY, int maxY) {
            this.minY = minY;
            this.maxY = maxY;
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

        static RgbColor average(RgbColor first, RgbColor second) {
            return new RgbColor(
                    (first.r + second.r) / 2,
                    (first.g + second.g) / 2,
                    (first.b + second.b) / 2);
        }
    }

    private static final class ColorModeFinder {
        private static final int COLOR_BIN_SHIFT = 4;
        private static final int COLOR_BIN_COUNT =
                1 << (3 * (8 - COLOR_BIN_SHIFT));

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
            for (int index = from; index < to; index++) {
                int rgb = compositeOnWhite(pixels[index]);
                int r = (rgb >>> 16) & 0xff;
                int g = (rgb >>> 8) & 0xff;
                int b = rgb & 0xff;
                int bin = ((r >>> COLOR_BIN_SHIFT)
                        << (2 * (8 - COLOR_BIN_SHIFT)))
                        | ((g >>> COLOR_BIN_SHIFT)
                        << (8 - COLOR_BIN_SHIFT))
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


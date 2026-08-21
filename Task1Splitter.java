import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

public class Task1Splitter {
    // 720px 폭 웹툰 기준 약 80px 이상의 여백만 컷 경계로 사용한다.
    private static final int MIN_GUTTER_HEIGHT = 80;
    private static final int MIN_DISTANCE_BETWEEN_CUTS = 250;

    // JPEG 노이즈를 허용하되 그림의 완만한 색 변화는 배경으로 보지 않는 값이다.
    private static final double BACKGROUND_DISTANCE = 18.0;
    private static final double MARGIN_COLOR_DISTANCE = 18.0;
    private static final double MAX_MARGIN_STD_DEV = 8.0;
    private static final double MAX_ROW_STD_DEV = 18.0;
    private static final double STRONG_BACKGROUND_RATIO = 0.94;
    private static final double STRONG_MARGIN_RATIO = 0.90;

    // 말풍선 꼬리나 작은 글자가 여백에 걸친 경우에 사용하는 느슨한 기준이다.
    private static final double WEAK_BACKGROUND_RATIO = 0.68;
    private static final double WEAK_MARGIN_RATIO = 0.75;
    private static final int MAX_STRONG_INTERRUPTION = 4;
    private static final int MAX_WEAK_EXPANSION = 80;
    private static final int MAX_OBSTRUCTION_HEIGHT = 180;

    public static void main(String[] args) {
        boolean dryRun = false;
        File outputRoot = new File("output");
        List<File> requestedInputs = new ArrayList<>();

        for (String arg : args) {
            if ("--dry-run".equals(arg)) {
                dryRun = true;
            } else if (arg.startsWith("--output=")) {
                outputRoot = new File(arg.substring("--output=".length()));
            } else {
                requestedInputs.add(new File(arg));
            }
        }

        if (requestedInputs.isEmpty()) {
            requestedInputs.add(new File("images"));
        }

        List<File> imageFiles = new ArrayList<>();
        for (File input : requestedInputs) {
            collectImageFiles(input, imageFiles);
        }
        imageFiles.sort(Comparator.comparing(File::getPath));

        if (imageFiles.isEmpty()) {
            System.err.println("처리할 이미지가 없습니다.");
            return;
        }

        for (File imageFile : imageFiles) {
            splitImage(imageFile, outputRoot, !dryRun);
        }
    }

    // 기존 코드에서 splitImage(File)을 호출하던 부분과의 호환성을 유지한다.
    static void splitImage(File imageFile) {
        splitImage(imageFile, new File("output"), true);
    }

    private static void splitImage(File imageFile, File outputRoot, boolean writeFiles) {
        try {
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                System.err.println("지원하지 않는 이미지입니다: " + imageFile.getPath());
                return;
            }

            List<Integer> cutYs = findCutPoints(image);
            System.out.println(imageFile.getPath() + " -> " + cutYs);

            if (!writeFiles) {
                return;
            }

            File parent = imageFile.getParentFile();
            String episode = parent == null ? "root" : parent.getName();
            File outputDirectory = new File(outputRoot, episode);
            if (!outputDirectory.isDirectory() && !outputDirectory.mkdirs()) {
                throw new IOException("출력 폴더를 만들 수 없습니다: " + outputDirectory.getPath());
            }

            String fileName = imageFile.getName();
            int dot = fileName.lastIndexOf('.');
            String baseName = dot < 0 ? fileName : fileName.substring(0, dot);

            for (int i = 0; i < cutYs.size() - 1; i++) {
                int top = cutYs.get(i);
                int bottom = cutYs.get(i + 1);
                BufferedImage piece = image.getSubimage(
                        0, top, image.getWidth(), bottom - top);
                File outputFile = new File(
                        outputDirectory, baseName + "_cut_" + i + ".jpg");
                if (!ImageIO.write(piece, "jpg", outputFile)) {
                    throw new IOException("JPEG writer를 찾을 수 없습니다.");
                }
            }
            removeStalePieces(outputDirectory, baseName, cutYs.size() - 1);
        } catch (IOException e) {
            System.err.println("이미지 처리 실패: " + imageFile.getPath());
            e.printStackTrace();
        }
    }

    static List<Integer> findCutPoints(BufferedImage image) {
        RowAnalysis rows = analyzeRows(image);
        boolean[] strongBlank = closeShortInterruptions(
                rows.strongBlank, MAX_STRONG_INTERRUPTION);

        List<SeparatorZone> zones = findSeparatorZones(strongBlank, rows.weakBlank);
        List<CutCandidate> candidates = new ArrayList<>();

        for (SeparatorZone zone : zones) {
            // 이미지 맨 위와 맨 아래의 바탕은 컷 사이의 여백이 아니다.
            if (zone.coreStart == 0 || zone.coreEnd == image.getHeight() - 1) {
                continue;
            }

            int cutY = chooseSafeMiddle(zone, strongBlank, rows);
            candidates.add(new CutCandidate(cutY, zone.strongRowCount));
        }

        List<Integer> cutYs = new ArrayList<>();
        cutYs.add(0);

        List<CutCandidate> separated = keepBestSeparatedCandidates(candidates);
        for (CutCandidate candidate : separated) {
            cutYs.add(candidate.y);
        }
        cutYs.add(image.getHeight());
        return cutYs;
    }

    // 이름은 남겨 두되, 더 이상 밝기 엣지만으로 판단하지 않는다.
    static List<Integer> findCutPointsByEdge(BufferedImage image) {
        return findCutPoints(image);
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

                if (colorDistanceSquared(r, g, b, background) <= backgroundDistanceSquared) {
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

            // weakBlank는 컷을 새로 만들지 않고 strong 구간의 경계 확장에만 쓴다.
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

        // 말풍선/글자는 행의 일부만 차지하므로 주변 배경 비율이 계속 높다.
        // 작은 그림 컷은 그렇지 않으므로 서로 다른 두 여백으로 남긴다.
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

        // zone에는 반드시 strong core가 있으므로 일반적으로 이 fallback은 실행되지 않는다.
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

    private static void collectImageFiles(File input, List<File> result) {
        if (input.isFile()) {
            if (isSupportedImage(input)) {
                result.add(input);
            }
            return;
        }

        File[] children = input.listFiles();
        if (children == null) {
            return;
        }
        Arrays.sort(children, Comparator.comparing(File::getName));
        for (File child : children) {
            collectImageFiles(child, result);
        }
    }

    private static boolean isSupportedImage(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".png");
    }

    private static void removeStalePieces(
            File outputDirectory, String baseName, int pieceCount) throws IOException {
        String prefix = baseName + "_cut_";
        File[] oldPieces = outputDirectory.listFiles((directory, name) ->
                name.startsWith(prefix) && name.toLowerCase(Locale.ROOT).endsWith(".jpg"));
        if (oldPieces == null) {
            return;
        }

        for (File oldPiece : oldPieces) {
            String name = oldPiece.getName();
            String indexText = name.substring(prefix.length(), name.length() - 4);
            try {
                int index = Integer.parseInt(indexText);
                if (index >= pieceCount && !oldPiece.delete()) {
                    throw new IOException("이전 출력 파일을 지울 수 없습니다: " + oldPiece.getPath());
                }
            } catch (NumberFormatException ignored) {
                // 같은 접두사지만 프로그램이 만든 형식이 아닌 파일은 건드리지 않는다.
            }
        }
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

}

final class RowAnalysis {
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

final class SeparatorZone {
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

final class CutCandidate {
        final int y;
        final int quality;

        CutCandidate(int y, int quality) {
            this.y = y;
            this.quality = quality;
        }
}

final class RgbColor {
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

final class ColorModeFinder {
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

package webtoon.core;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** 베이스라인의 핵심만 남긴 여백 탐지: 가장자리 색과 비슷한 행을 여백으로 본다. */
public final class BlankRows {
    private static final int COLOR_TOLERANCE = 30;
    private static final double MAX_FOREGROUND_RATIO = 0.02;

    private BlankRows() {}

    public static List<BlankRun> find(BufferedImage image, int minimumHeight) {
        List<BlankRun> result = new ArrayList<>();
        int start = -1;

        for (int y = 0; y <= image.getHeight(); y++) {
            boolean blank = y < image.getHeight() && isBlank(image, y);
            if (blank && start < 0) start = y;
            if (!blank && start >= 0) {
                if (y - start >= minimumHeight) result.add(new BlankRun(start, y));
                start = -1;
            }
        }
        return result;
    }

    private static boolean isBlank(BufferedImage image, int y) {
        int width = image.getWidth();
        int edge = Math.max(1, Math.min(width / 20, 20));
        int background = averageEdgeColor(image, y, edge);
        int foreground = 0;

        for (int x = 0; x < width; x++) {
            if (distance(image.getRGB(x, y), background) > COLOR_TOLERANCE) foreground++;
        }
        return foreground <= width * MAX_FOREGROUND_RATIO;
    }

    private static int averageEdgeColor(BufferedImage image, int y, int edge) {
        long red = 0, green = 0, blue = 0;
        for (int i = 0; i < edge; i++) {
            int left = image.getRGB(i, y);
            int right = image.getRGB(image.getWidth() - 1 - i, y);
            red += ((left >> 16) & 255) + ((right >> 16) & 255);
            green += ((left >> 8) & 255) + ((right >> 8) & 255);
            blue += (left & 255) + (right & 255);
        }
        int count = edge * 2;
        return (int) (red / count) << 16 | (int) (green / count) << 8 | (int) (blue / count);
    }

    private static int distance(int first, int second) {
        int red = Math.abs(((first >> 16) & 255) - ((second >> 16) & 255));
        int green = Math.abs(((first >> 8) & 255) - ((second >> 8) & 255));
        int blue = Math.abs((first & 255) - (second & 255));
        return Math.max(red, Math.max(green, blue));
    }
}

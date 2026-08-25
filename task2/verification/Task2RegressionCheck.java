package task2.verification;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import task2.baseline.ContentRegion;
import task2.baseline.Task2Baseline;

/** 사용자가 확인한 Task2 오분할 여섯 건이 다시 합쳐지지 않는지 검사한다. */
public final class Task2RegressionCheck {
    private static final RegressionCase[] CASES = {
        new RegressionCase(
                "images/001/002.jpg",
                9,
                new int[] {5500, 6450, 7100},
                new int[] {6177, 6810}),
        new RegressionCase(
                "images/003/003.jpg",
                8,
                new int[] {5000, 6500},
                new int[] {6260}),
        new RegressionCase(
                "images/005/003.jpg",
                8,
                new int[] {3300, 4500},
                new int[] {3901}),
        new RegressionCase(
                "images/006/001.jpg",
                7,
                new int[] {4300, 7200},
                new int[] {6389}),
        new RegressionCase(
                "images/006/002.jpg",
                7,
                new int[] {500, 2350, 3500, 4700, 5500, 7000, 8000},
                new int[] {1838, 2800, 4400, 5000, 6600, 7400, 9500}),
        new RegressionCase(
                "images/006/008.jpg",
                5,
                new int[] {2800, 4250},
                new int[] {3618})
    };

    /** 짧은 흰 구간을 지나도 하나의 선으로 이어지는 말풍선은 합쳐 둔다. */
    private static final ContinuityCase[] CONTINUITY_CASES = {
        new ContinuityCase("images/006/004.jpg", 8, 3460, 3530),
        new ContinuityCase("images/007/005.jpg", 8, 3350, 3420)
    };

    /** 분할은 유지하되 말풍선/효과선의 끝부분이 경계에서 잘리지 않아야 한다. */
    private static final PreservationCase[] PRESERVATION_CASES = {
        new PreservationCase(
                "images/004/001.jpg", 8,
                new int[] {438, 2214, 2710},
                new int[] {460, 2450}),
        new PreservationCase(
                "images/011/003.jpg", 11,
                new int[] {3625, 4358},
                new int[] {4000}),
        new PreservationCase(
                "images/012/003.jpg", 11,
                new int[] {6220, 7500},
                new int[] {6800})
    };

    private Task2RegressionCheck() {
    }

    public static void main(String[] args) throws IOException {
        ImageIO.setUseCache(false);
        for (RegressionCase regression : CASES) {
            verify(regression);
        }
        for (ContinuityCase continuity : CONTINUITY_CASES) {
            verifyContinuity(continuity);
        }
        for (PreservationCase preservation : PRESERVATION_CASES) {
            verifyPreservation(preservation);
        }
        System.out.println(
                "PASS: 6개 오분할, 2개 연결성, 3개 경계 보존 검사를 통과했습니다.");
    }

    private static void verify(RegressionCase regression) throws IOException {
        File source = new File(regression.path);
        BufferedImage image = ImageIO.read(source);
        if (image == null) {
            throw new IOException("검증 이미지를 읽을 수 없습니다: " + source);
        }

        List<ContentRegion> regions = Task2Baseline.findContentRegions(image);
        if (regions.size() != regression.expectedRegionCount) {
            throw new AssertionError(
                    source + " 결과 개수: expected="
                            + regression.expectedRegionCount
                            + ", actual=" + regions.size()
                            + ", regions=" + regions);
        }

        int previousRegion = -1;
        for (int y : regression.mustKeepYs) {
            int region = containingRegion(regions, y);
            if (region < 0) {
                throw new AssertionError(
                        source + "에서 보존해야 할 y=" + y
                                + "가 삭제됐습니다: " + regions);
            }
            if (region <= previousRegion) {
                throw new AssertionError(
                        source + "의 핵심 콘텐츠가 서로 분리되지 않았습니다: "
                                + Arrays.toString(regression.mustKeepYs)
                                + " -> " + regions);
            }
            previousRegion = region;
        }

        for (int y : regression.mustDiscardYs) {
            if (containingRegion(regions, y) >= 0) {
                throw new AssertionError(
                        source + "에서 제거해야 할 여백 y=" + y
                                + "가 남았습니다: " + regions);
            }
        }
    }

    private static int containingRegion(List<ContentRegion> regions, int y) {
        for (int index = 0; index < regions.size(); index++) {
            ContentRegion region = regions.get(index);
            if (region.top() <= y && y < region.bottomExclusive()) {
                return index;
            }
        }
        return -1;
    }

    private static void verifyContinuity(ContinuityCase continuity) throws IOException {
        File source = new File(continuity.path);
        BufferedImage image = ImageIO.read(source);
        if (image == null) {
            throw new IOException("검증 이미지를 읽을 수 없습니다: " + source);
        }

        List<ContentRegion> regions = Task2Baseline.findContentRegions(image);
        if (regions.size() != continuity.expectedRegionCount) {
            throw new AssertionError(
                    source + " 결과 개수: expected="
                            + continuity.expectedRegionCount
                            + ", actual=" + regions.size()
                            + ", regions=" + regions);
        }

        int upperRegion = containingRegion(regions, continuity.upperY);
        int lowerRegion = containingRegion(regions, continuity.lowerY);
        if (upperRegion < 0 || upperRegion != lowerRegion) {
            throw new AssertionError(
                    source + "의 이어진 말풍선/그림이 분리됐습니다: y="
                            + continuity.upperY + ", " + continuity.lowerY
                            + " -> " + regions);
        }
    }

    private static void verifyPreservation(PreservationCase preservation)
            throws IOException {
        File source = new File(preservation.path);
        BufferedImage image = ImageIO.read(source);
        if (image == null) {
            throw new IOException("검증 이미지를 읽을 수 없습니다: " + source);
        }

        List<ContentRegion> regions = Task2Baseline.findContentRegions(image);
        if (regions.size() != preservation.expectedRegionCount) {
            throw new AssertionError(
                    source + " 결과 개수: expected="
                            + preservation.expectedRegionCount
                            + ", actual=" + regions.size()
                            + ", regions=" + regions);
        }
        for (int y : preservation.mustKeepYs) {
            if (containingRegion(regions, y) < 0) {
                throw new AssertionError(
                        source + "에서 경계 그림선 y=" + y
                                + "가 잘렸습니다: " + regions);
            }
        }
        for (int y : preservation.mustDiscardYs) {
            if (containingRegion(regions, y) >= 0) {
                throw new AssertionError(
                        source + "에서 순수 여백 y=" + y
                                + "가 남았습니다: " + regions);
            }
        }
    }

    private static final class RegressionCase {
        final String path;
        final int expectedRegionCount;
        final int[] mustKeepYs;
        final int[] mustDiscardYs;

        RegressionCase(
                String path,
                int expectedRegionCount,
                int[] mustKeepYs,
                int[] mustDiscardYs) {
            this.path = path;
            this.expectedRegionCount = expectedRegionCount;
            this.mustKeepYs = mustKeepYs;
            this.mustDiscardYs = mustDiscardYs;
        }
    }

    private static final class ContinuityCase {
        final String path;
        final int expectedRegionCount;
        final int upperY;
        final int lowerY;

        ContinuityCase(String path, int expectedRegionCount, int upperY, int lowerY) {
            this.path = path;
            this.expectedRegionCount = expectedRegionCount;
            this.upperY = upperY;
            this.lowerY = lowerY;
        }
    }

    private static final class PreservationCase {
        final String path;
        final int expectedRegionCount;
        final int[] mustKeepYs;
        final int[] mustDiscardYs;

        PreservationCase(
                String path,
                int expectedRegionCount,
                int[] mustKeepYs,
                int[] mustDiscardYs) {
            this.path = path;
            this.expectedRegionCount = expectedRegionCount;
            this.mustKeepYs = mustKeepYs;
            this.mustDiscardYs = mustDiscardYs;
        }
    }
}

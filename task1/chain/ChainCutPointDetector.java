package task1.chain;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 연속 여백 후보를 찾고 Handler 연쇄에 넘기는 책임 연쇄의 Client입니다.
 */
public final class ChainCutPointDetector {
    private final RgbMarginAnalyzer analyzer;
    private final BlankZoneHandler firstHandler;

    public ChainCutPointDetector() {
        analyzer = new RgbMarginAnalyzer();

        MinimumHeightHandler height =
                new MinimumHeightHandler(analyzer);
        ColoredStabilityHandler stability =
                new ColoredStabilityHandler(analyzer);
        SafeCutSelectionHandler safeCut =
                new SafeCutSelectionHandler(analyzer);

        // 높이 검사 → 컬러 안정성 검사 → 안전 cutY 선택 순서로 연결합니다.
        height.setNext(stability).setNext(safeCut);
        firstHandler = height;
    }

    public List<Integer> findCutPoints(BufferedImage image) {
        Objects.requireNonNull(image, "image");

        List<Integer> cutPoints = new ArrayList<>();
        cutPoints.add(0);
        int blankStart = -1;

        for (int y = 0; y < image.getHeight(); y++) {
            boolean blank = analyzer.classifyBlankRow(image, y)
                    != RgbMarginAnalyzer.CONTENT_ROW;

            if (blank && blankStart == -1) {
                blankStart = y;
            }

            if (!blank && blankStart != -1) {
                handleCandidate(
                        image, cutPoints, blankStart, y - 1);
                blankStart = -1;
            }
        }

        if (blankStart != -1) {
            handleCandidate(
                    image,
                    cutPoints,
                    blankStart,
                    image.getHeight() - 1);
        }

        cutPoints.add(image.getHeight());
        return cutPoints;
    }

    private void handleCandidate(
            BufferedImage image,
            List<Integer> cutPoints,
            int start,
            int end) {

        BlankZoneContext context =
                new BlankZoneContext(image, start, end);
        if (firstHandler.handle(context)) {
            cutPoints.add(context.getCutY());
        }
    }
}

package task1.chain;

/** 두 번째 책임: 컬러 여백의 세로 방향 색 변화가 안정적인지 검사합니다. */
public final class ColoredStabilityHandler extends BlankZoneHandler {
    private final RgbMarginAnalyzer analyzer;

    public ColoredStabilityHandler(RgbMarginAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    @Override
    protected boolean check(BlankZoneContext context) {
        for (int[] run : context.getColoredRuns()) {
            if (analyzer.isStableColoredBackground(
                    context.getImage(), run[0], run[1])) {
                context.setColoredStable(true);
                break;
            }
        }

        // 무채색 높이가 충분하거나, 컬러 구간이 안정적일 때만 다음 책임으로 갑니다.
        return context.isNeutralHeightQualified()
                || context.isColoredStable();
    }
}

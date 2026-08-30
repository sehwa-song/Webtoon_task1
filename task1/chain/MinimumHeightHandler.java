package task1.chain;

/** 첫 번째 책임: 무채색/컬러 여백이 최소 높이를 만족하는지 검사합니다. */
public final class MinimumHeightHandler extends BlankZoneHandler {
    private final RgbMarginAnalyzer analyzer;

    public MinimumHeightHandler(RgbMarginAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    @Override
    protected boolean check(BlankZoneContext context) {
        if (context.getStartY() < 0
                || context.getEndY() < context.getStartY()
                || context.getStartY() == 0
                || context.getEndY()
                        == context.getImage().getHeight() - 1) {
            return false;
        }

        int neutralRunLength = 0;
        int coloredRunStart = -1;

        for (int y = context.getStartY();
                y <= context.getEndY();
                y++) {

            int rowType = analyzer.classifyBlankRow(
                    context.getImage(), y);

            if (rowType == RgbMarginAnalyzer.NEUTRAL_BLANK_ROW) {
                neutralRunLength++;
                if (neutralRunLength
                        >= analyzer.getMinimumNeutralBlankHeight()) {
                    context.setNeutralHeightQualified(true);
                }

                if (coloredRunStart != -1) {
                    addColoredRunWhenTallEnough(
                            context, coloredRunStart, y - 1);
                    coloredRunStart = -1;
                }
            } else if (rowType
                    == RgbMarginAnalyzer.COLORED_BLANK_ROW) {
                neutralRunLength = 0;
                if (coloredRunStart == -1) {
                    coloredRunStart = y;
                }
            }
        }

        if (coloredRunStart != -1) {
            addColoredRunWhenTallEnough(
                    context,
                    coloredRunStart,
                    context.getEndY());
        }

        return context.isNeutralHeightQualified()
                || !context.getColoredRuns().isEmpty();
    }

    private void addColoredRunWhenTallEnough(
            BlankZoneContext context,
            int start,
            int end) {

        if (end - start + 1
                >= analyzer.getMinimumColoredBlankHeight()) {
            context.addColoredRun(start, end);
        }
    }
}

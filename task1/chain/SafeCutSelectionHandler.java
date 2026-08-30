package task1.chain;

/** 세 번째 책임: 말풍선/글자 픽셀이 가장 적은 안전한 cutY를 선택합니다. */
public final class SafeCutSelectionHandler extends BlankZoneHandler {
    private final RgbMarginAnalyzer analyzer;

    public SafeCutSelectionHandler(RgbMarginAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    @Override
    protected boolean check(BlankZoneContext context) {
        int cutY = analyzer.findSafestCutY(
                context.getImage(),
                context.getStartY(),
                context.getEndY());
        context.setCutY(cutY);
        return cutY != -1;
    }
}

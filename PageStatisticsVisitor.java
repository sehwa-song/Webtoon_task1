/** 노드 클래스 변경 없이 페이지 통계 연산을 추가할 수 있음을 보여 주는 두 번째 Visitor. */
final class PageStatisticsVisitor implements WebtoonVisitor {
    private int episodeCount;
    private int pageCount;
    private long totalPixels;
    private int maximumHeight;

    @Override
    public void enterBatch(WebtoonBatch batch) {
        episodeCount = 0;
        pageCount = 0;
        totalPixels = 0;
        maximumHeight = 0;
    }

    @Override
    public void enterEpisode(WebtoonEpisode episode) {
        episodeCount++;
    }

    @Override
    public void visitPage(WebtoonPage page) {
        pageCount++;
        totalPixels += (long) page.image().getWidth() * page.image().getHeight();
        maximumHeight = Math.max(maximumHeight, page.image().getHeight());
    }

    @Override
    public void leaveEpisode(WebtoonEpisode episode) {
        // 후처리 없음
    }

    @Override
    public void leaveBatch(WebtoonBatch batch) {
        // 후처리 없음
    }

    int episodeCount() {
        return episodeCount;
    }

    int pageCount() {
        return pageCount;
    }

    long totalPixels() {
        return totalPixels;
    }

    int maximumHeight() {
        return maximumHeight;
    }
}

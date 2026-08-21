/** Batch, Episode, Page에 새로운 연산을 추가하는 Visitor 인터페이스. */
public interface WebtoonVisitor {
    void enterBatch(WebtoonBatch batch);

    void enterEpisode(WebtoonEpisode episode);

    void visitPage(WebtoonPage page);

    void leaveEpisode(WebtoonEpisode episode);

    void leaveBatch(WebtoonBatch batch);
}

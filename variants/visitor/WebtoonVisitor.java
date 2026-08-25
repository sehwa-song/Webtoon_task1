package variants.visitor;

import java.io.IOException;

/** Batch, Episode, Page 타입마다 다른 연산을 추가하는 Visitor 계약이다. */
interface WebtoonVisitor {
    void enterBatch(WebtoonBatch batch) throws IOException;

    void enterEpisode(WebtoonEpisode episode) throws IOException;

    void visitPage(WebtoonPage page) throws IOException;

    void leaveEpisode(WebtoonEpisode episode) throws IOException;

    void leaveBatch(WebtoonBatch batch) throws IOException;
}

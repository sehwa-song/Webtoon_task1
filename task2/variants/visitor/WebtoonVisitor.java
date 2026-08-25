package task2.variants.visitor;

import java.io.IOException;

/** 웹툰 계층의 각 종류를 방문할 때 실행할 작업의 규칙이다. */
public interface WebtoonVisitor {
    void enterBatch(WebtoonBatch batch) throws IOException;

    void enterEpisode(WebtoonEpisode episode) throws IOException;

    void visitPage(WebtoonPage page) throws IOException;

    void leaveEpisode(WebtoonEpisode episode) throws IOException;

    void leaveBatch(WebtoonBatch batch) throws IOException;
}

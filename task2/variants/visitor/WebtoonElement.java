package task2.variants.visitor;

import java.io.IOException;

/** Composite의 공통 Component이면서 Visitor의 방문 대상 Element이다. */
public interface WebtoonElement {
    void accept(WebtoonVisitor visitor) throws IOException;
}

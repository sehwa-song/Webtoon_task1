package variants.visitor;

import java.io.IOException;

/** Visitor가 순회할 Composite 요소의 공통 계약이다. */
interface WebtoonElement {
    void accept(WebtoonVisitor visitor) throws IOException;
}

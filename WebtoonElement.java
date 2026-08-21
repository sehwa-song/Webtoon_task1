/** Visitor가 순회할 수 있는 웹툰 객체 구조의 공통 타입. */
interface WebtoonElement {
    void accept(WebtoonVisitor visitor);
}

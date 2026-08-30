package task1.visitor;

/** Composite의 공통 Component이면서 Visitor를 받을 수 있는 Element입니다. */
public interface WebtoonElement {
    void accept(WebtoonVisitor visitor);
}

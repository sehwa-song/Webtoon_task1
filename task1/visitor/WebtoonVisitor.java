package task1.visitor;

/** 각 Composite/Leaf 타입에 맞는 visit 메서드를 제공하는 Visitor 인터페이스입니다. */
public interface WebtoonVisitor {
    void visit(WebtoonBatch batch);
    void visit(WebtoonEpisode episode);
    void visit(WebtoonPage page);
}

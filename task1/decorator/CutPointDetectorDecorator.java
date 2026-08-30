package task1.decorator;

import java.util.Objects;

/** 다른 CutPointDetector를 감싸는 모든 Decorator의 공통 부모입니다. */
public abstract class CutPointDetectorDecorator implements CutPointDetector {
    protected final CutPointDetector delegate;

    protected CutPointDetectorDecorator(CutPointDetector delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }
}

package task1.strategy;

import java.io.File;

/** HybridRowClassifier 전략을 독립 실행하는 기본 Client입니다. */
public final class HybridStrategyApp {
    private HybridStrategyApp() {
    }

    public static void main(String[] args) {
        StrategyAppRunner.run(
                args,
                new HybridRowClassifier(),
                new File("output-task1-patterns/strategy/hybrid"));
    }
}

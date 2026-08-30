package task1.strategy;

import java.io.File;

/** FixedColorRowClassifier 전략을 독립 실행하는 Client입니다. */
public final class FixedColorStrategyApp {
    private FixedColorStrategyApp() {
    }

    public static void main(String[] args) {
        StrategyAppRunner.run(
                args,
                new FixedColorRowClassifier(),
                new File("output-task1-patterns/strategy/fixed"));
    }
}

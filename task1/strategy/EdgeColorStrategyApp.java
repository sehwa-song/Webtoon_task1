package task1.strategy;

import java.io.File;

/** EdgeColorRowClassifier 전략을 독립 실행하는 Client입니다. */
public final class EdgeColorStrategyApp {
    private EdgeColorStrategyApp() {
    }

    public static void main(String[] args) {
        StrategyAppRunner.run(
                args,
                new EdgeColorRowClassifier(),
                new File("output-task1-patterns/strategy/edge"));
    }
}

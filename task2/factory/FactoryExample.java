package task2.factory;

import task2.core.ContentAlgorithm;
import task2.core.SimpleContentAlgorithm;
import task2.core.Task2Runner;

public final class FactoryExample {
    static final class ContentAlgorithmFactory {
        ContentAlgorithm create() { return new SimpleContentAlgorithm(); }
    }

    public static void main(String[] args) throws Exception {
        ContentAlgorithm algorithm = new ContentAlgorithmFactory().create();
        Task2Runner.run("Factory", algorithm, args);
    }
}

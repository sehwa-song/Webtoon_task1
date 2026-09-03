package task1.factory;

import task1.core.CutAlgorithm;
import task1.core.SimpleCutAlgorithm;
import task1.core.Task1Runner;

public final class FactoryExample {
    static final class CutAlgorithmFactory {
        CutAlgorithm create() { return new SimpleCutAlgorithm(); }
    }

    public static void main(String[] args) throws Exception {
        CutAlgorithm algorithm = new CutAlgorithmFactory().create();
        Task1Runner.run("Factory", algorithm, args);
    }
}

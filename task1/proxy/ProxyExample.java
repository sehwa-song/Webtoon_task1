package task1.proxy;

import java.awt.image.BufferedImage;
import java.util.List;
import task1.core.CutAlgorithm;
import task1.core.SimpleCutAlgorithm;
import task1.core.Task1Runner;

public final class ProxyExample {
    static final class CutProxy implements CutAlgorithm {
        private final CutAlgorithm realAlgorithm;
        CutProxy(CutAlgorithm realAlgorithm) { this.realAlgorithm = realAlgorithm; }
        public List<Integer> detectCuts(BufferedImage image) {
            return realAlgorithm.detectCuts(image);
        }
    }

    public static void main(String[] args) throws Exception {
        Task1Runner.run("Proxy", new CutProxy(new SimpleCutAlgorithm()), args);
    }
}

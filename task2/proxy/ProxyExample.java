package task2.proxy;

import java.awt.image.BufferedImage;
import java.util.List;
import task2.core.ContentAlgorithm;
import task2.core.ContentRegion;
import task2.core.SimpleContentAlgorithm;
import task2.core.Task2Runner;

public final class ProxyExample {
    static final class ContentProxy implements ContentAlgorithm {
        private final ContentAlgorithm realAlgorithm;
        ContentProxy(ContentAlgorithm realAlgorithm) { this.realAlgorithm = realAlgorithm; }
        public List<ContentRegion> detectContent(BufferedImage image) {
            return realAlgorithm.detectContent(image);
        }
    }

    public static void main(String[] args) throws Exception {
        Task2Runner.run("Proxy", new ContentProxy(new SimpleContentAlgorithm()), args);
    }
}

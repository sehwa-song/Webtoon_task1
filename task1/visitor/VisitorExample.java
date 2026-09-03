package task1.visitor;

import java.awt.image.BufferedImage;
import java.util.List;
import task1.core.CutAlgorithm;
import task1.core.SimpleCutAlgorithm;
import task1.core.Task1Runner;

public final class VisitorExample {
    interface ImageVisitor {
        List<Integer> visit(WebtoonImage image);
    }

    static final class WebtoonImage {
        private final BufferedImage pixels;
        WebtoonImage(BufferedImage pixels) { this.pixels = pixels; }
        BufferedImage pixels() { return pixels; }
        List<Integer> accept(ImageVisitor visitor) { return visitor.visit(this); }
    }

    static final class CutVisitor implements ImageVisitor {
        private final CutAlgorithm algorithm = new SimpleCutAlgorithm();
        public List<Integer> visit(WebtoonImage image) {
            return algorithm.detectCuts(image.pixels());
        }
    }

    static final class VisitorCutAlgorithm implements CutAlgorithm {
        public List<Integer> detectCuts(BufferedImage image) {
            return new WebtoonImage(image).accept(new CutVisitor());
        }
    }

    public static void main(String[] args) throws Exception {
        Task1Runner.run("Visitor", new VisitorCutAlgorithm(), args);
    }
}

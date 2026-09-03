package task2.visitor;

import java.awt.image.BufferedImage;
import java.util.List;
import task2.core.ContentAlgorithm;
import task2.core.ContentRegion;
import task2.core.SimpleContentAlgorithm;
import task2.core.Task2Runner;

public final class VisitorExample {
    interface ImageVisitor {
        List<ContentRegion> visit(WebtoonImage image);
    }

    static final class WebtoonImage {
        private final BufferedImage pixels;
        WebtoonImage(BufferedImage pixels) { this.pixels = pixels; }
        BufferedImage pixels() { return pixels; }
        List<ContentRegion> accept(ImageVisitor visitor) { return visitor.visit(this); }
    }

    static final class ContentVisitor implements ImageVisitor {
        private final ContentAlgorithm algorithm = new SimpleContentAlgorithm();
        public List<ContentRegion> visit(WebtoonImage image) {
            return algorithm.detectContent(image.pixels());
        }
    }

    static final class VisitorContentAlgorithm implements ContentAlgorithm {
        public List<ContentRegion> detectContent(BufferedImage image) {
            return new WebtoonImage(image).accept(new ContentVisitor());
        }
    }

    public static void main(String[] args) throws Exception {
        Task2Runner.run("Visitor", new VisitorContentAlgorithm(), args);
    }
}

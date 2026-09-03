package task2.decorator;

import java.awt.image.BufferedImage;
import java.util.List;
import task2.core.ContentAlgorithm;
import task2.core.ContentRegion;
import task2.core.Regions;
import task2.core.SimpleContentAlgorithm;
import task2.core.Task2Runner;
import webtoon.core.BlankRows;

public final class DecoratorExample {
    static final class WholeImage implements ContentAlgorithm {
        public List<ContentRegion> detectContent(BufferedImage image) {
            return List.of(new ContentRegion(0, image.getHeight()));
        }
    }

    static final class BlankRemovalDecorator implements ContentAlgorithm {
        private final ContentAlgorithm original;
        BlankRemovalDecorator(ContentAlgorithm original) { this.original = original; }

        public List<ContentRegion> detectContent(BufferedImage image) {
            List<ContentRegion> base = original.detectContent(image);
            if (base.isEmpty()) return base;
            return Regions.withoutBlank(image.getHeight(),
                    BlankRows.find(image, SimpleContentAlgorithm.MINIMUM_BLANK_HEIGHT));
        }
    }

    public static void main(String[] args) throws Exception {
        ContentAlgorithm algorithm = new BlankRemovalDecorator(new WholeImage());
        Task2Runner.run("Decorator", algorithm, args);
    }
}

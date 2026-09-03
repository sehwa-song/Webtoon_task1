package task1.decorator;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import task1.core.CutAlgorithm;
import task1.core.SimpleCutAlgorithm;
import task1.core.Task1Runner;
import webtoon.core.BlankRun;
import webtoon.core.BlankRows;

public final class DecoratorExample {
    static final class ImageBoundary implements CutAlgorithm {
        public List<Integer> detectCuts(BufferedImage image) {
            return List.of(0, image.getHeight());
        }
    }

    static final class BlankCutDecorator implements CutAlgorithm {
        private final CutAlgorithm original;
        BlankCutDecorator(CutAlgorithm original) { this.original = original; }

        public List<Integer> detectCuts(BufferedImage image) {
            List<Integer> cuts = new ArrayList<>(original.detectCuts(image));
            for (BlankRun blank : BlankRows.find(image, SimpleCutAlgorithm.MINIMUM_BLANK_HEIGHT)) {
                if (blank.top() > 0 && blank.bottom() < image.getHeight()) cuts.add(blank.middle());
            }
            cuts.sort(Integer::compareTo);
            return cuts;
        }
    }

    public static void main(String[] args) throws Exception {
        CutAlgorithm algorithm = new BlankCutDecorator(new ImageBoundary());
        Task1Runner.run("Decorator", algorithm, args);
    }
}

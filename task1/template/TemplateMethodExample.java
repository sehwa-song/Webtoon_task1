package task1.template;

import java.awt.image.BufferedImage;
import java.util.List;
import task1.core.CutAlgorithm;
import task1.core.Cuts;
import task1.core.SimpleCutAlgorithm;
import task1.core.Task1Runner;
import webtoon.core.BlankRun;
import webtoon.core.BlankRows;

public final class TemplateMethodExample {
    abstract static class CutTemplate implements CutAlgorithm {
        public final List<Integer> detectCuts(BufferedImage image) {
            List<BlankRun> blanks = findBlankRows(image);
            return makeCutPoints(image.getHeight(), blanks);
        }
        protected abstract List<BlankRun> findBlankRows(BufferedImage image);
        protected abstract List<Integer> makeCutPoints(int height, List<BlankRun> blanks);
    }

    static final class BaselineCutTemplate extends CutTemplate {
        protected List<BlankRun> findBlankRows(BufferedImage image) {
            return BlankRows.find(image, SimpleCutAlgorithm.MINIMUM_BLANK_HEIGHT);
        }
        protected List<Integer> makeCutPoints(int height, List<BlankRun> blanks) {
            return Cuts.from(height, blanks);
        }
    }

    public static void main(String[] args) throws Exception {
        Task1Runner.run("Template Method", new BaselineCutTemplate(), args);
    }
}

package task2.template;

import java.awt.image.BufferedImage;
import java.util.List;
import task2.core.ContentAlgorithm;
import task2.core.ContentRegion;
import task2.core.Regions;
import task2.core.SimpleContentAlgorithm;
import task2.core.Task2Runner;
import webtoon.core.BlankRun;
import webtoon.core.BlankRows;

public final class TemplateMethodExample {
    abstract static class ContentTemplate implements ContentAlgorithm {
        public final List<ContentRegion> detectContent(BufferedImage image) {
            List<BlankRun> blanks = findBlankRows(image);
            return removeBlankRows(image.getHeight(), blanks);
        }
        protected abstract List<BlankRun> findBlankRows(BufferedImage image);
        protected abstract List<ContentRegion> removeBlankRows(int height, List<BlankRun> blanks);
    }

    static final class BaselineContentTemplate extends ContentTemplate {
        protected List<BlankRun> findBlankRows(BufferedImage image) {
            return BlankRows.find(image, SimpleContentAlgorithm.MINIMUM_BLANK_HEIGHT);
        }
        protected List<ContentRegion> removeBlankRows(int height, List<BlankRun> blanks) {
            return Regions.withoutBlank(height, blanks);
        }
    }

    public static void main(String[] args) throws Exception {
        Task2Runner.run("Template Method", new BaselineContentTemplate(), args);
    }
}

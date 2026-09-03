package task2.chain;

import java.awt.image.BufferedImage;
import java.util.List;
import task2.core.ContentAlgorithm;
import task2.core.ContentRegion;
import task2.core.Regions;
import task2.core.SimpleContentAlgorithm;
import task2.core.Task2Runner;
import webtoon.core.BlankRun;
import webtoon.core.BlankRows;

public final class ChainExample {
    static final class Context {
        final BufferedImage image;
        List<BlankRun> blanks;
        List<ContentRegion> regions;
        Context(BufferedImage image) { this.image = image; }
    }

    abstract static class Handler {
        private Handler next;
        Handler next(Handler value) { next = value; return value; }
        final void handle(Context context) {
            execute(context);
            if (next != null) next.handle(context);
        }
        protected abstract void execute(Context context);
    }

    static final class FindBlankRows extends Handler {
        protected void execute(Context c) {
            c.blanks = BlankRows.find(c.image, SimpleContentAlgorithm.MINIMUM_BLANK_HEIGHT);
        }
    }

    static final class MakeContentRegions extends Handler {
        protected void execute(Context c) {
            c.regions = Regions.withoutBlank(c.image.getHeight(), c.blanks);
        }
    }

    static final class ChainContentAlgorithm implements ContentAlgorithm {
        private final Handler first;
        ChainContentAlgorithm() {
            first = new FindBlankRows();
            first.next(new MakeContentRegions());
        }
        public List<ContentRegion> detectContent(BufferedImage image) {
            Context context = new Context(image);
            first.handle(context);
            return context.regions;
        }
    }

    public static void main(String[] args) throws Exception {
        Task2Runner.run("Chain of Responsibility", new ChainContentAlgorithm(), args);
    }
}

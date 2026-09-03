package task1.chain;

import java.awt.image.BufferedImage;
import java.util.List;
import task1.core.CutAlgorithm;
import task1.core.Cuts;
import task1.core.SimpleCutAlgorithm;
import task1.core.Task1Runner;
import webtoon.core.BlankRun;
import webtoon.core.BlankRows;

public final class ChainExample {
    static final class Context {
        final BufferedImage image;
        List<BlankRun> blanks;
        List<Integer> cuts;
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
            c.blanks = BlankRows.find(c.image, SimpleCutAlgorithm.MINIMUM_BLANK_HEIGHT);
        }
    }

    static final class MakeCutPoints extends Handler {
        protected void execute(Context c) { c.cuts = Cuts.from(c.image.getHeight(), c.blanks); }
    }

    static final class ChainCutAlgorithm implements CutAlgorithm {
        private final Handler first;
        ChainCutAlgorithm() {
            first = new FindBlankRows();
            first.next(new MakeCutPoints());
        }
        public List<Integer> detectCuts(BufferedImage image) {
            Context context = new Context(image);
            first.handle(context);
            return context.cuts;
        }
    }

    public static void main(String[] args) throws Exception {
        Task1Runner.run("Chain of Responsibility", new ChainCutAlgorithm(), args);
    }
}

package task1.core;

import java.util.ArrayList;
import java.util.List;
import webtoon.core.BlankRun;

public final class Cuts {
    private Cuts() {}

    public static List<Integer> from(int imageHeight, List<BlankRun> blankRuns) {
        List<Integer> cuts = new ArrayList<>();
        cuts.add(0);
        for (BlankRun run : blankRuns) {
            if (run.top() > 0 && run.bottom() < imageHeight) cuts.add(run.middle());
        }
        cuts.add(imageHeight);
        return cuts;
    }
}

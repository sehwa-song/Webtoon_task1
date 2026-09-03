package task2.core;

import java.util.ArrayList;
import java.util.List;
import webtoon.core.BlankRun;

public final class Regions {
    private Regions() {}

    /** 여백 구간을 빼고 남는 구간만 반환한다. */
    public static List<ContentRegion> withoutBlank(int imageHeight, List<BlankRun> blankRuns) {
        List<ContentRegion> regions = new ArrayList<>();
        int top = 0;
        for (BlankRun blank : blankRuns) {
            if (top < blank.top()) regions.add(new ContentRegion(top, blank.top()));
            top = blank.bottom();
        }
        if (top < imageHeight) regions.add(new ContentRegion(top, imageHeight));
        return regions;
    }
}

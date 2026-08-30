package task1.visitor;

import java.io.File;
import java.util.Collections;
import java.util.List;

/** 한 WebtoonPage 방문 결과입니다. */
public final class PageVisitResult {
    public final File source;
    public final List<Integer> cutPoints;
    public final long loadNanos;
    public final long detectNanos;
    public final long writeNanos;
    public final long totalNanos;

    PageVisitResult(
            File source,
            List<Integer> cutPoints,
            long loadNanos,
            long detectNanos,
            long writeNanos,
            long totalNanos) {
        this.source = source;
        this.cutPoints = Collections.unmodifiableList(cutPoints);
        this.loadNanos = loadNanos;
        this.detectNanos = detectNanos;
        this.writeNanos = writeNanos;
        this.totalNanos = totalNanos;
    }

    public int pieceCount() {
        return cutPoints.size() - 1;
    }
}

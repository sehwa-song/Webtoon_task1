package task1.factory;

import java.io.File;
import java.util.Collections;
import java.util.List;

/** 한 이미지 처리 결과와 구간별 실행시간을 보관합니다. */
public final class ProcessingResult {
    public final File source;
    public final List<Integer> cutPoints;
    public final long loadNanos;
    public final long detectNanos;
    public final long writeNanos;
    public final long totalNanos;

    public ProcessingResult(
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

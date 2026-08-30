package task1.template;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Template Method 한 번의 이미지 처리 결과와 단계별 시간을 보관합니다. */
public final class TemplateSplitResult {
    private final File source;
    private final List<Integer> cutPoints;
    private final long loadNanos;
    private final long detectNanos;
    private final long writeNanos;
    private final long totalNanos;

    public TemplateSplitResult(
            File source,
            List<Integer> cutPoints,
            long loadNanos,
            long detectNanos,
            long writeNanos,
            long totalNanos) {

        this.source = source;
        this.cutPoints = Collections.unmodifiableList(
                new ArrayList<>(cutPoints));
        this.loadNanos = loadNanos;
        this.detectNanos = detectNanos;
        this.writeNanos = writeNanos;
        this.totalNanos = totalNanos;
    }

    public File getSource() {
        return source;
    }

    public List<Integer> getCutPoints() {
        return cutPoints;
    }

    public int getPieceCount() {
        return cutPoints.size() - 1;
    }

    public long getLoadNanos() {
        return loadNanos;
    }

    public long getDetectNanos() {
        return detectNanos;
    }

    public long getWriteNanos() {
        return writeNanos;
    }

    public long getTotalNanos() {
        return totalNanos;
    }
}

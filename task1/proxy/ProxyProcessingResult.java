package task1.proxy;

import java.util.List;

/** 이미지 한 장의 결과와 단계별 실행 시간을 보관합니다. */
public final class ProxyProcessingResult {
    private final List<Integer> cutPoints;
    private final long loadNanos;
    private final long detectNanos;
    private final long writeNanos;
    private final long totalNanos;

    public ProxyProcessingResult(List<Integer> cutPoints, long loadNanos,
            long detectNanos, long writeNanos, long totalNanos) {
        this.cutPoints = List.copyOf(cutPoints);
        this.loadNanos = loadNanos;
        this.detectNanos = detectNanos;
        this.writeNanos = writeNanos;
        this.totalNanos = totalNanos;
    }

    public List<Integer> getCutPoints() {
        return cutPoints;
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

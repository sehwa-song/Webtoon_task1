package task1.decorator;

import java.awt.image.BufferedImage;
import java.util.List;

/** 핵심 알고리즘을 수정하지 않고 호출 횟수와 탐색 시간을 누적합니다. */
public final class TimingCutPointDetectorDecorator
        extends CutPointDetectorDecorator {

    private long callCount;
    private long totalNanos;

    public TimingCutPointDetectorDecorator(CutPointDetector delegate) {
        super(delegate);
    }

    @Override
    public List<Integer> findCutPoints(BufferedImage image) {
        long start = System.nanoTime();
        try {
            return delegate.findCutPoints(image);
        } finally {
            totalNanos += System.nanoTime() - start;
            callCount++;
        }
    }

    public long getCallCount() {
        return callCount;
    }

    public long getTotalNanos() {
        return totalNanos;
    }

    @Override
    public String name() {
        return "timing(" + delegate.name() + ")";
    }
}

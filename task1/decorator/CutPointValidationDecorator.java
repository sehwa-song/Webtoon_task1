package task1.decorator;

import java.awt.image.BufferedImage;
import java.util.List;

/** 핵심 알고리즘을 수정하지 않고 반환된 컷 좌표의 안전성을 검사합니다. */
public final class CutPointValidationDecorator
        extends CutPointDetectorDecorator {

    public CutPointValidationDecorator(CutPointDetector delegate) {
        super(delegate);
    }

    @Override
    public List<Integer> findCutPoints(BufferedImage image) {
        List<Integer> points = delegate.findCutPoints(image);

        if (points == null || points.size() < 2) {
            throw new IllegalStateException("컷 좌표가 두 개 이상 필요합니다.");
        }
        if (points.get(0) != 0) {
            throw new IllegalStateException("첫 좌표는 반드시 0이어야 합니다.");
        }
        if (points.get(points.size() - 1) != image.getHeight()) {
            throw new IllegalStateException("마지막 좌표는 이미지 높이여야 합니다.");
        }

        for (int i = 1; i < points.size(); i++) {
            if (points.get(i) <= points.get(i - 1)) {
                throw new IllegalStateException("컷 좌표는 오름차순이어야 합니다: " + points);
            }
        }
        return List.copyOf(points);
    }

    @Override
    public String name() {
        return "validation(" + delegate.name() + ")";
    }
}

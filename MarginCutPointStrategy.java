import java.awt.image.BufferedImage;
import java.util.List;

/** 현재 Task1Splitter 알고리즘을 감싼 무상태 Singleton Strategy. */
final class MarginCutPointStrategy implements CutPointStrategy {
    private MarginCutPointStrategy() {
    }

    private static final class Holder {
        private static final MarginCutPointStrategy INSTANCE =
                new MarginCutPointStrategy();
    }

    static MarginCutPointStrategy getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public List<Integer> findCutPoints(BufferedImage image) {
        return Task1Splitter.findCutPoints(image);
    }

    @Override
    public String name() {
        return "margin-rgb";
    }
}

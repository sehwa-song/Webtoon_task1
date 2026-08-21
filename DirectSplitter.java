import java.awt.image.BufferedImage;
import java.util.List;

/** 디자인 패턴을 거치지 않고 Task1Splitter 코어를 직접 호출하는 기준선. */
final class DirectSplitter {
    List<Integer> findCutPoints(BufferedImage image) {
        return Task1Splitter.findCutPoints(image);
    }
}

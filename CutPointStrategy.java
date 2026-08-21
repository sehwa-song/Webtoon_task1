import java.awt.image.BufferedImage;
import java.util.List;

/** 컷 검출 알고리즘을 교체할 수 있게 만드는 Strategy 인터페이스. */
public interface CutPointStrategy {
    List<Integer> findCutPoints(BufferedImage image);

    String name();
}

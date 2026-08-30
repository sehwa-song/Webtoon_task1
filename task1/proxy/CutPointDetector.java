package task1.proxy;

import java.awt.image.BufferedImage;
import java.util.List;

/** 이 Proxy 예제를 독립 실행하기 위한 Task1 컷 탐색 인터페이스입니다. */
public interface CutPointDetector {
    List<Integer> findCutPoints(BufferedImage image);
}

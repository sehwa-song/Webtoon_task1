package task1.decorator;

import java.awt.image.BufferedImage;
import java.util.List;

/** 컷 위치를 찾는 원본 객체와 Decorator가 함께 구현하는 공통 인터페이스입니다. */
public interface CutPointDetector {
    List<Integer> findCutPoints(BufferedImage image);

    String name();
}

package task1.strategy;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Strategy 패턴의 공통 인터페이스입니다.
 * 교체되는 알고리즘 단위는 "현재 y행을 어떤 종류의 행으로 볼 것인가"입니다.
 */
public interface RowClassifier {
    RowType classify(BufferedImage image, int y);

    /** 말풍선이나 글자를 피할 안전한 cutY를 고를 때 사용하는 전경 픽셀 수입니다. */
    int countForegroundPixels(BufferedImage image, int y);

    /** 컬러 여백의 세로 색 안정성을 검사할 때 사용하는 예상 배경색입니다. */
    Color findExpectedBackgroundColor(BufferedImage image, int y);

    String name();
}

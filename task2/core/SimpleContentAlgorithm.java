package task2.core;

import java.awt.image.BufferedImage;
import java.util.List;
import webtoon.core.BlankRows;

/** Task2 핵심: 긴 여백을 찾아 제외하고 그림이 있는 구간만 남긴다. */
public final class SimpleContentAlgorithm implements ContentAlgorithm {
    public static final int MINIMUM_BLANK_HEIGHT = 48;

    @Override
    public List<ContentRegion> detectContent(BufferedImage image) {
        return Regions.withoutBlank(image.getHeight(), BlankRows.find(image, MINIMUM_BLANK_HEIGHT));
    }
}

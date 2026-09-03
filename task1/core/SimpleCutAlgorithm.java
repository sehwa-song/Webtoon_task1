package task1.core;

import java.awt.image.BufferedImage;
import java.util.List;
import webtoon.core.BlankRows;

/** Task1 핵심: 긴 여백을 찾고 그 가운데를 절단점으로 정한다. */
public final class SimpleCutAlgorithm implements CutAlgorithm {
    public static final int MINIMUM_BLANK_HEIGHT = 50;

    @Override
    public List<Integer> detectCuts(BufferedImage image) {
        return Cuts.from(image.getHeight(), BlankRows.find(image, MINIMUM_BLANK_HEIGHT));
    }
}

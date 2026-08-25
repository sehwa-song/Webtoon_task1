package variants.strategy;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Strategy contract for finding horizontal cut positions in one webtoon image.
 *
 * <p>The returned list includes {@code 0} and {@code image.getHeight()} so that
 * every adjacent pair describes one output piece.</p>
 */
public interface CutPointStrategy { // (전략 (추상화된 알고리즘))
    List<Integer> findCutPoints(BufferedImage image);
}

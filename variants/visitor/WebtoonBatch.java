package variants.visitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 여러 에피소드를 담는 Composite 최상위 요소다. */
final class WebtoonBatch implements WebtoonElement {
    private final List<WebtoonEpisode> episodes;

    WebtoonBatch(List<WebtoonEpisode> episodes) {
        this.episodes = Collections.unmodifiableList(new ArrayList<>(episodes));
    }

    List<WebtoonEpisode> episodes() {
        return episodes;
    }

    @Override
    public void accept(WebtoonVisitor visitor) throws IOException {
        visitor.enterBatch(this);
        for (WebtoonEpisode episode : episodes) {
            episode.accept(visitor);
        }
        visitor.leaveBatch(this);
    }
}

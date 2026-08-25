package task2.variants.visitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** 여러 Episode를 포함하는 Composite 최상위 객체이다. */
public final class WebtoonBatch implements WebtoonElement {
    private final List<WebtoonEpisode> episodes;

    public WebtoonBatch(List<WebtoonEpisode> episodes) {
        Objects.requireNonNull(episodes, "episodes");
        this.episodes = Collections.unmodifiableList(
                new ArrayList<WebtoonEpisode>(episodes));
    }

    public List<WebtoonEpisode> episodes() {
        return episodes;
    }

    @Override
    public void accept(WebtoonVisitor visitor) throws IOException {
        Objects.requireNonNull(visitor, "visitor");
        visitor.enterBatch(this);
        for (WebtoonEpisode episode : episodes) {
            episode.accept(visitor);
        }
        visitor.leaveBatch(this);
    }
}

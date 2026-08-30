package task1.visitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 여러 에피소드를 담는 최상위 Composite입니다. */
public final class WebtoonBatch implements WebtoonElement {
    private final String name;
    private final List<WebtoonEpisode> episodes = new ArrayList<>();

    public WebtoonBatch(String name) {
        this.name = name;
    }

    public void add(WebtoonEpisode episode) {
        episodes.add(episode);
    }

    public String getName() {
        return name;
    }

    public List<WebtoonEpisode> getEpisodes() {
        return Collections.unmodifiableList(episodes);
    }

    @Override
    public void accept(WebtoonVisitor visitor) {
        visitor.visit(this);
        for (WebtoonEpisode episode : episodes) {
            episode.accept(visitor);
        }
    }
}

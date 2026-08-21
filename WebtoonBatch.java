import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 여러 에피소드를 포함하는 Composite 최상위 요소. */
final class WebtoonBatch implements WebtoonElement {
    private final List<WebtoonEpisode> episodes;

    WebtoonBatch(List<WebtoonEpisode> episodes) {
        this.episodes = Collections.unmodifiableList(new ArrayList<>(episodes));
    }

    List<WebtoonEpisode> episodes() {
        return episodes;
    }

    List<WebtoonPage> pages() {
        List<WebtoonPage> allPages = new ArrayList<>();
        for (WebtoonEpisode episode : episodes) {
            allPages.addAll(episode.pages());
        }
        return allPages;
    }

    @Override
    public void accept(WebtoonVisitor visitor) {
        visitor.enterBatch(this);
        for (WebtoonEpisode episode : episodes) {
            episode.accept(visitor);
        }
        visitor.leaveBatch(this);
    }
}

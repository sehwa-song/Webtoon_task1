import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 실제 images 아래 에피소드 폴더 하나에 대응하는 중간 요소. */
final class WebtoonEpisode implements WebtoonElement {
    private final String name;
    private final List<WebtoonPage> pages;

    WebtoonEpisode(String name, List<WebtoonPage> pages) {
        this.name = name;
        this.pages = Collections.unmodifiableList(new ArrayList<>(pages));
    }

    String name() {
        return name;
    }

    List<WebtoonPage> pages() {
        return pages;
    }

    @Override
    public void accept(WebtoonVisitor visitor) {
        visitor.enterEpisode(this);
        for (WebtoonPage page : pages) {
            page.accept(visitor);
        }
        visitor.leaveEpisode(this);
    }
}

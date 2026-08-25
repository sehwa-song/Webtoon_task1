package variants.visitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 한 에피소드와 그 페이지를 묶는 Composite 중간 요소다. */
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
    public void accept(WebtoonVisitor visitor) throws IOException {
        visitor.enterEpisode(this);
        for (WebtoonPage page : pages) {
            page.accept(visitor);
        }
        visitor.leaveEpisode(this);
    }
}

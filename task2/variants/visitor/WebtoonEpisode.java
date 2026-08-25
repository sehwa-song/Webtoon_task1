package task2.variants.visitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** 한 Episode와 그 Page들을 포함하는 중간 Composite 객체이다. */
public final class WebtoonEpisode implements WebtoonElement {
    private final String name;
    private final List<WebtoonPage> pages;

    public WebtoonEpisode(String name, List<WebtoonPage> pages) {
        this.name = Objects.requireNonNull(name, "name");
        Objects.requireNonNull(pages, "pages");
        this.pages = Collections.unmodifiableList(
                new ArrayList<WebtoonPage>(pages));
    }

    public String name() {
        return name;
    }

    public List<WebtoonPage> pages() {
        return pages;
    }

    @Override
    public void accept(WebtoonVisitor visitor) throws IOException {
        Objects.requireNonNull(visitor, "visitor");
        visitor.enterEpisode(this);
        for (WebtoonPage page : pages) {
            page.accept(visitor);
        }
        visitor.leaveEpisode(this);
    }
}

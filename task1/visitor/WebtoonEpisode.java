package task1.visitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 여러 페이지를 담는 중간 Composite입니다. */
public final class WebtoonEpisode implements WebtoonElement {
    private final String name;
    private final List<WebtoonPage> pages = new ArrayList<>();

    public WebtoonEpisode(String name) {
        this.name = name;
    }

    public void add(WebtoonPage page) {
        pages.add(page);
    }

    public String getName() {
        return name;
    }

    public List<WebtoonPage> getPages() {
        return Collections.unmodifiableList(pages);
    }

    @Override
    public void accept(WebtoonVisitor visitor) {
        visitor.visit(this);
        for (WebtoonPage page : pages) {
            page.accept(visitor);
        }
    }
}

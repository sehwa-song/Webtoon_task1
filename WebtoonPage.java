import java.awt.image.BufferedImage;
import java.nio.file.Path;

/** 이미지 한 장을 나타내는 leaf 요소. */
final class WebtoonPage implements WebtoonElement {
    private final Path path;
    private final BufferedImage image;

    WebtoonPage(Path path, BufferedImage image) {
        this.path = path;
        this.image = image;
    }

    Path path() {
        return path;
    }

    BufferedImage image() {
        return image;
    }

    @Override
    public void accept(WebtoonVisitor visitor) {
        visitor.visitPage(this);
    }
}

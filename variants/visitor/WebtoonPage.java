package variants.visitor;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import javax.imageio.ImageIO;

/** 실제 이미지 한 장을 나타내는 Composite leaf 요소다. */
final class WebtoonPage implements WebtoonElement {
    private final Path path;
    private final BufferedImage loadedImage;

    private WebtoonPage(Path path, BufferedImage loadedImage) {
        this.path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        this.loadedImage = loadedImage;
    }

    static WebtoonPage fromPath(Path path) {
        return new WebtoonPage(path, null);
    }

    static WebtoonPage fromLoadedImage(Path path, BufferedImage image) {
        return new WebtoonPage(path, Objects.requireNonNull(image, "image"));
    }

    Path path() {
        return path;
    }

    BufferedImage loadImage() throws IOException {
        if (loadedImage != null) {
            return loadedImage;
        }

        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) {
            throw new IOException("지원하지 않는 이미지입니다: " + path);
        }
        return image;
    }

    @Override
    public void accept(WebtoonVisitor visitor) throws IOException {
        visitor.visitPage(this);
    }
}

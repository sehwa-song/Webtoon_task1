package task2.variants.visitor;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import javax.imageio.ImageIO;

/** 실제 이미지 한 장을 나타내며 자식을 갖지 않는 Composite Leaf이다. */
public final class WebtoonPage implements WebtoonElement {
    private final Path path;
    private final BufferedImage loadedImage;

    private WebtoonPage(Path path, BufferedImage loadedImage) {
        this.path = Objects.requireNonNull(path, "path")
                .toAbsolutePath().normalize();
        this.loadedImage = loadedImage;
    }

    public static WebtoonPage fromPath(Path path) {
        return new WebtoonPage(path, null);
    }

    public static WebtoonPage fromLoadedImage(Path path, BufferedImage image) {
        return new WebtoonPage(path, Objects.requireNonNull(image, "image"));
    }

    public Path path() {
        return path;
    }

    public BufferedImage loadImage() throws IOException {
        if (loadedImage != null) {
            return loadedImage;
        }
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) {
            throw new IOException("지원하지 않거나 손상된 이미지: " + path);
        }
        return image;
    }

    @Override
    public void accept(WebtoonVisitor visitor) throws IOException {
        Objects.requireNonNull(visitor, "visitor").visitPage(this);
    }
}

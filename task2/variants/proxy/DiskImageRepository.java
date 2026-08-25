package task2.variants.proxy;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import javax.imageio.ImageIO;

/** RealSubject that decodes every requested image directly from disk. */
public final class DiskImageRepository implements ImageRepository {
    public DiskImageRepository() {
    }

    @Override
    public BufferedImage load(Path source) throws IOException {
        Path normalized = Objects.requireNonNull(source, "source")
                .toAbsolutePath().normalize();
        BufferedImage image = ImageIO.read(normalized.toFile());
        if (image == null) {
            throw new IOException("Unsupported or damaged image: " + normalized);
        }
        return image;
    }
}

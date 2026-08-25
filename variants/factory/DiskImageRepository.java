package variants.factory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Standard-family repository: every request is decoded from disk. */
final class DiskImageRepository implements ImageRepository {
    @Override
    public BufferedImage load(Path source) throws IOException {
        Path normalized = source.toAbsolutePath().normalize();
        BufferedImage image = ImageIO.read(normalized.toFile());
        if (image == null) {
            throw new IOException("Unsupported or damaged image: " + normalized);
        }
        return image;
    }
}

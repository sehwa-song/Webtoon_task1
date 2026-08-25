package task2.variants.factory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/** Abstract product responsible for loading source images. */
public interface ImageRepository {
    BufferedImage load(Path source) throws IOException;
}

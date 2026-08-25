package variants.factory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/** Loads a source image for the processing service. */
public interface ImageRepository {
    BufferedImage load(Path source) throws IOException;
}

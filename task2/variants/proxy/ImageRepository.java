package task2.variants.proxy;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Subject in the Proxy pattern. Clients load images through this contract and
 * therefore do not need to know whether a caching proxy is present.
 */
public interface ImageRepository {
    BufferedImage load(Path source) throws IOException;
}

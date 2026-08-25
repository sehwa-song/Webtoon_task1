package task2.variants.factory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Abstract product that writes the preserved content regions. */
public interface RegionWriter {
    List<Path> writeRegions(
            Path source,
            BufferedImage image,
            List<ContentRegion> regions,
            Path outputRoot) throws IOException;

    String name();
}

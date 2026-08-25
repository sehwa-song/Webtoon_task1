package task2.variants.proxy;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Writes the preserved content regions produced by the detector. */
public interface RegionWriter {
    List<Path> writeRegions(
            Path source,
            BufferedImage image,
            List<ContentRegion> regions,
            Path outputRoot) throws IOException;

    String name();
}


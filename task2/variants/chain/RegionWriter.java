package task2.variants.chain;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Output contract used by the standalone chain extractor. */
public interface RegionWriter {
    List<Path> writeRegions(
            Path source,
            BufferedImage image,
            List<ContentRegion> regions,
            Path outputRoot) throws IOException;

    String name();
}


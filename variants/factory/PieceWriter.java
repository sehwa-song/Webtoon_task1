package variants.factory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Writes the pieces described by consecutive cut points. */
public interface PieceWriter {
    List<Path> writePieces(
            Path source,
            BufferedImage image,
            List<Integer> cutPoints,
            Path outputRoot) throws IOException;

    String name();
}

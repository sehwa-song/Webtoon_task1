package variants.factory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;

/** Pure-Java JPEG writer used by both product families. */
final class JpegPieceWriter implements PieceWriter {
    private JpegPieceWriter() {
    }

    private static final class Holder {
        private static final JpegPieceWriter INSTANCE = new JpegPieceWriter();
    }

    static JpegPieceWriter getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public List<Path> writePieces(
            Path source,
            BufferedImage image,
            List<Integer> cutPoints,
            Path outputRoot) throws IOException {
        validateCuts(image, cutPoints);

        Path normalizedSource = source.toAbsolutePath().normalize();
        Path parent = normalizedSource.getParent();
        String episode = parent == null
                ? "root"
                : parent.getFileName().toString();
        Path outputDirectory = outputRoot.toAbsolutePath().normalize().resolve(episode);
        Files.createDirectories(outputDirectory);

        String fileName = normalizedSource.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String baseName = dot < 0 ? fileName : fileName.substring(0, dot);

        List<Path> written = new ArrayList<Path>();
        for (int index = 0; index < cutPoints.size() - 1; index++) {
            int top = cutPoints.get(index);
            int bottom = cutPoints.get(index + 1);
            BufferedImage piece = toRgbPiece(image, top, bottom);
            Path destination = outputDirectory.resolve(
                    baseName + "_cut_" + index + ".jpg");
            if (!ImageIO.write(piece, "jpg", destination.toFile())) {
                throw new IOException("No JPEG writer is available: " + destination);
            }
            written.add(destination);
        }
        return Collections.unmodifiableList(written);
    }

    @Override
    public String name() {
        return "jpeg-piece-writer";
    }

    private static BufferedImage toRgbPiece(
            BufferedImage source, int top, int bottom) {
        int height = bottom - top;
        BufferedImage piece = new BufferedImage(
                source.getWidth(), height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = piece.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, piece.getWidth(), piece.getHeight());
            graphics.drawImage(
                    source,
                    0,
                    0,
                    source.getWidth(),
                    height,
                    0,
                    top,
                    source.getWidth(),
                    bottom,
                    null);
        } finally {
            graphics.dispose();
        }
        return piece;
    }

    private static void validateCuts(
            BufferedImage image, List<Integer> cutPoints) {
        if (cutPoints == null || cutPoints.size() < 2) {
            throw new IllegalArgumentException("At least two cut points are required");
        }
        if (cutPoints.get(0) != 0
                || cutPoints.get(cutPoints.size() - 1) != image.getHeight()) {
            throw new IllegalArgumentException(
                    "Cut points must start at zero and end at image height");
        }
        int previous = -1;
        for (int cut : cutPoints) {
            if (cut <= previous || cut < 0 || cut > image.getHeight()) {
                throw new IllegalArgumentException(
                        "Cut points must be strictly increasing and in bounds: "
                                + cutPoints);
            }
            previous = cut;
        }
    }
}

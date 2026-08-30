package task1.factory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/** 컷 범위를 RGB JPEG 파일로 저장하는 제품입니다. */
public final class JpegPieceWriter implements PieceWriter {
    private static final JpegPieceWriter INSTANCE = new JpegPieceWriter();

    private JpegPieceWriter() {
    }

    public static JpegPieceWriter getInstance() {
        return INSTANCE;
    }

    @Override
    public List<File> write(
            File sourceFile,
            BufferedImage image,
            List<Integer> cutPoints,
            File outputRoot) throws IOException {

        File parent = sourceFile.getParentFile();
        File directory = new File(
                outputRoot,
                parent == null ? "root" : parent.getName());
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("출력 폴더 생성 실패: " + directory);
        }

        String name = sourceFile.getName();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        List<File> written = new ArrayList<>();

        for (int i = 0; i < cutPoints.size() - 1; i++) {
            int top = cutPoints.get(i);
            int bottom = cutPoints.get(i + 1);
            if (bottom <= top) {
                throw new IOException("잘못된 컷 범위: " + top + "~" + bottom);
            }
            BufferedImage piece = toRgbPiece(image, top, bottom);
            File destination = new File(
                    directory,
                    base + "_cut_" + String.format(Locale.ROOT, "%03d", i)
                            + ".jpg");
            if (!ImageIO.write(piece, "jpg", destination)) {
                throw new IOException("JPEG 저장기를 찾을 수 없습니다.");
            }
            written.add(destination);
        }
        removeStale(directory, base, written.size());
        return Collections.unmodifiableList(written);
    }

    private static BufferedImage toRgbPiece(
            BufferedImage source, int top, int bottom) {
        BufferedImage piece = new BufferedImage(
                source.getWidth(), bottom - top, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = piece.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, piece.getWidth(), piece.getHeight());
            graphics.drawImage(
                    source,
                    0, 0, source.getWidth(), bottom - top,
                    0, top, source.getWidth(), bottom,
                    null);
        } finally {
            graphics.dispose();
        }
        return piece;
    }

    private static void removeStale(File directory, String base, int count)
            throws IOException {
        String prefix = base + "_cut_";
        File[] files = directory.listFiles((dir, fileName) ->
                fileName.startsWith(prefix)
                        && fileName.toLowerCase(Locale.ROOT).endsWith(".jpg"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            String number = file.getName().substring(
                    prefix.length(), file.getName().length() - 4);
            try {
                if (Integer.parseInt(number) >= count && !file.delete()) {
                    throw new IOException("이전 결과 삭제 실패: " + file);
                }
            } catch (NumberFormatException ignored) {
                // 현재 이름 규칙이 아닌 파일은 보존합니다.
            }
        }
    }

    @Override
    public String name() {
        return "jpeg-piece-writer";
    }
}

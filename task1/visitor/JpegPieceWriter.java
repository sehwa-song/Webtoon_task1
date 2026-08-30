package task1.visitor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/** Visitor 패키지만으로 실행할 수 있도록 포함한 JPEG 저장기입니다. */
final class JpegPieceWriter {
    void write(
            File source,
            BufferedImage image,
            List<Integer> cuts,
            File outputRoot) throws IOException {
        File parent = source.getParentFile();
        File directory = new File(
                outputRoot, parent == null ? "root" : parent.getName());
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("출력 폴더 생성 실패: " + directory);
        }

        String sourceName = source.getName();
        int dot = sourceName.lastIndexOf('.');
        String base = dot > 0 ? sourceName.substring(0, dot) : sourceName;
        for (int i = 0; i < cuts.size() - 1; i++) {
            int top = cuts.get(i);
            int bottom = cuts.get(i + 1);
            BufferedImage piece = new BufferedImage(
                    image.getWidth(), bottom - top, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = piece.createGraphics();
            try {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, piece.getWidth(), piece.getHeight());
                graphics.drawImage(
                        image,
                        0, 0, image.getWidth(), bottom - top,
                        0, top, image.getWidth(), bottom,
                        null);
            } finally {
                graphics.dispose();
            }
            File output = new File(
                    directory,
                    base + "_cut_" + String.format(Locale.ROOT, "%03d", i)
                            + ".jpg");
            if (!ImageIO.write(piece, "jpg", output)) {
                throw new IOException("JPEG 저장기를 찾을 수 없습니다.");
            }
        }
        removeStale(directory, base, cuts.size() - 1);
    }

    private static void removeStale(File directory, String base, int count)
            throws IOException {
        String prefix = base + "_cut_";
        File[] files = directory.listFiles((dir, name) ->
                name.startsWith(prefix)
                        && name.toLowerCase(Locale.ROOT).endsWith(".jpg"));
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
                // 프로그램의 이름 규칙과 다른 파일은 보존합니다.
            }
        }
    }
}

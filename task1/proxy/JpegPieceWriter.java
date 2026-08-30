package task1.proxy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/** 컷 좌표 사이의 영역을 JPEG 파일로 저장합니다. */
public final class JpegPieceWriter {
    public int write(File sourceFile, BufferedImage image,
            List<Integer> points, File outputRoot) throws IOException {

        File parent = sourceFile.getParentFile();
        File directory = new File(outputRoot,
                parent == null ? "root" : parent.getName());
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("출력 폴더를 만들 수 없습니다: " + directory);
        }

        String fileName = sourceFile.getName();
        int dot = fileName.lastIndexOf('.');
        String baseName = dot > 0 ? fileName.substring(0, dot) : fileName;

        for (int i = 0; i < points.size() - 1; i++) {
            int top = points.get(i);
            int bottom = points.get(i + 1);
            BufferedImage piece = toRgbPiece(image, top, bottom);
            File destination = new File(directory, baseName + "_cut_"
                    + String.format(Locale.ROOT, "%03d", i) + ".jpg");
            if (!ImageIO.write(piece, "jpg", destination)) {
                throw new IOException("JPEG 저장기를 찾지 못했습니다.");
            }
        }
        removeStalePieces(directory, baseName, points.size() - 1);
        return points.size() - 1;
    }

    private static BufferedImage toRgbPiece(
            BufferedImage source, int top, int bottom) {
        BufferedImage result = new BufferedImage(source.getWidth(),
                bottom - top, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, result.getWidth(), result.getHeight());
            graphics.drawImage(source, 0, 0, source.getWidth(), result.getHeight(),
                    0, top, source.getWidth(), bottom, null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private static void removeStalePieces(File directory, String baseName,
            int currentCount) throws IOException {
        String prefix = baseName + "_cut_";
        File[] files = directory.listFiles((dir, name) ->
                name.startsWith(prefix) && name.toLowerCase(Locale.ROOT).endsWith(".jpg"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            String number = file.getName().substring(prefix.length(),
                    file.getName().length() - 4);
            try {
                if (Integer.parseInt(number) >= currentCount && !file.delete()) {
                    throw new IOException("이전 출력 파일 삭제 실패: " + file);
                }
            } catch (NumberFormatException ignored) {
                // 이 프로그램의 번호 형식이 아닌 파일은 보존합니다.
            }
        }
    }
}


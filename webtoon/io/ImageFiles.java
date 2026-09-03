package webtoon.io;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** 패턴과 무관한 파일 읽기·자르기·저장만 담당한다. */
public final class ImageFiles {
    private ImageFiles() {}

    public static BufferedImage read(Path input) throws IOException {
        BufferedImage image = ImageIO.read(input.toFile());
        if (image == null) throw new IOException("이미지 파일이 아닙니다: " + input);
        return image;
    }

    public static void savePart(BufferedImage source, int top, int bottom, Path output)
            throws IOException {
        Files.createDirectories(output.getParent());
        BufferedImage rgb = new BufferedImage(source.getWidth(), bottom - top, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
        graphics.drawImage(source, 0, -top, null);
        graphics.dispose();
        ImageIO.write(rgb, "jpg", output.toFile());
    }

    public static String baseName(Path input) {
        String name = input.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }
}

package task1.template;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * Template Method 패턴의 추상 클래스입니다.
 *
 * <p>{@link #split(File, File, boolean)}은 로딩 → 컷 탐색 → 저장 순서를
 * final로 고정합니다. 구체 클래스는 {@link #detectCutPoints(BufferedImage)}
 * 훅만 구현하므로 전체 작업 순서를 실수로 바꿀 수 없습니다.</p>
 */
public abstract class AbstractWebtoonSplitter {

    /** 전체 처리 순서를 고정한 Template Method입니다. */
    public final TemplateSplitResult split(
            File imageFile,
            File outputRoot,
            boolean dryRun) throws IOException {

        long totalStart = System.nanoTime();

        long loadStart = System.nanoTime();
        BufferedImage image = loadImage(imageFile);
        long loadNanos = System.nanoTime() - loadStart;

        long detectStart = System.nanoTime();
        List<Integer> cutPoints = detectCutPoints(image);
        long detectNanos = System.nanoTime() - detectStart;

        long writeNanos = 0L;
        if (!dryRun) {
            long writeStart = System.nanoTime();
            writePieces(imageFile, image, cutPoints, outputRoot);
            writeNanos = System.nanoTime() - writeStart;
        }

        return new TemplateSplitResult(
                imageFile,
                cutPoints,
                loadNanos,
                detectNanos,
                writeNanos,
                System.nanoTime() - totalStart);
    }

    /** 알고리즘마다 달라지는 부분이므로 구체 클래스가 구현하는 훅입니다. */
    protected abstract List<Integer> detectCutPoints(
            BufferedImage image);

    /** 공통 로딩 단계입니다. 필요하면 하위 클래스가 재정의할 수 있습니다. */
    protected BufferedImage loadImage(File imageFile) throws IOException {
        BufferedImage image = ImageIO.read(imageFile);
        if (image == null) {
            throw new IOException(
                    "지원하지 않거나 읽을 수 없는 이미지입니다: "
                    + imageFile.getPath());
        }
        return image;
    }

    /** 공통 저장 단계입니다. */
    protected void writePieces(
            File imageFile,
            BufferedImage image,
            List<Integer> cutPoints,
            File outputRoot) throws IOException {

        File parent = imageFile.getParentFile();
        String episode = parent == null ? "root" : parent.getName();
        File outputDirectory = new File(outputRoot, episode);

        if (!outputDirectory.isDirectory()
                && !outputDirectory.mkdirs()) {
            throw new IOException(
                    "출력 폴더를 만들 수 없습니다: "
                    + outputDirectory.getPath());
        }

        String fileName = imageFile.getName();
        int dot = fileName.lastIndexOf('.');
        String baseName = dot > 0
                ? fileName.substring(0, dot)
                : fileName;

        for (int i = 0; i < cutPoints.size() - 1; i++) {
            int top = cutPoints.get(i);
            int bottom = cutPoints.get(i + 1);
            if (bottom <= top) {
                throw new IOException(
                        "잘못된 컷 범위입니다: " + top + "~" + bottom);
            }

            BufferedImage piece = toRgbPiece(image, top, bottom);
            File destination = new File(
                    outputDirectory,
                    baseName + "_cut_"
                    + String.format(Locale.ROOT, "%03d", i)
                    + ".jpg");

            if (!ImageIO.write(piece, "jpg", destination)) {
                throw new IOException("JPEG 저장기를 찾을 수 없습니다.");
            }
        }

        removeStalePieces(
                outputDirectory,
                baseName,
                cutPoints.size() - 1);
    }

    private static BufferedImage toRgbPiece(
            BufferedImage source,
            int top,
            int bottom) {

        BufferedImage piece = new BufferedImage(
                source.getWidth(),
                bottom - top,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = piece.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, piece.getWidth(), piece.getHeight());
            graphics.drawImage(
                    source,
                    0,
                    0,
                    source.getWidth(),
                    piece.getHeight(),
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

    private static void removeStalePieces(
            File directory,
            String baseName,
            int pieceCount) throws IOException {

        String prefix = baseName + "_cut_";
        File[] oldPieces = directory.listFiles((ignored, name) ->
                name.startsWith(prefix)
                && name.toLowerCase(Locale.ROOT).endsWith(".jpg"));
        if (oldPieces == null) {
            return;
        }

        for (File oldPiece : oldPieces) {
            String name = oldPiece.getName();
            String number = name.substring(
                    prefix.length(),
                    name.length() - ".jpg".length());
            try {
                if (Integer.parseInt(number) >= pieceCount
                        && !oldPiece.delete()) {
                    throw new IOException(
                            "이전 결과를 삭제할 수 없습니다: "
                            + oldPiece.getPath());
                }
            } catch (NumberFormatException ignored) {
                // 현재 프로그램의 이름 규칙이 아닌 파일은 삭제하지 않습니다.
            }
        }
    }
}

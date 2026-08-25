package variants.visitor;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;

/** Page 방문 시 이미지 로드, 컷 검출, 선택적 JPG 저장을 수행하는 Concrete Visitor다. */
final class SplitPageVisitor implements WebtoonVisitor {
    private final Path outputRoot;
    private final boolean writeFiles;
    private final boolean printResults;
    private final Map<Path, List<Integer>> results = new LinkedHashMap<>();
    private Path currentOutputDirectory;

    SplitPageVisitor(Path outputRoot, boolean writeFiles, boolean printResults) {
        this.outputRoot = outputRoot.toAbsolutePath().normalize();
        this.writeFiles = writeFiles;
        this.printResults = printResults;
    }

    @Override
    public void enterBatch(WebtoonBatch batch) {
        results.clear();
        currentOutputDirectory = null;
    }

    @Override
    public void enterEpisode(WebtoonEpisode episode) throws IOException {
        currentOutputDirectory = outputRoot.resolve(episode.name());
        if (writeFiles) {
            Files.createDirectories(currentOutputDirectory);
        }
    }

    @Override
    public void visitPage(WebtoonPage page) throws IOException {
        BufferedImage image = page.loadImage();

        // 실제 알고리즘 실행은 반드시 Page의 accept -> visitPage 경로 안에서 일어난다.
        List<Integer> cutYs = Collections.unmodifiableList(
                new ArrayList<>(VisitorCutAlgorithm.findCutPoints(image)));
        results.put(page.path(), cutYs);

        if (printResults) {
            System.out.println(page.path() + " -> " + cutYs);
        }
        if (writeFiles) {
            writePieces(page, image, cutYs);
        }
    }

    @Override
    public void leaveEpisode(WebtoonEpisode episode) {
        currentOutputDirectory = null;
    }

    @Override
    public void leaveBatch(WebtoonBatch batch) {
        // 배치 단위 후처리가 필요하면 이 지점에 추가할 수 있다.
    }

    Map<Path, List<Integer>> results() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(results));
    }

    long checksum() {
        long checksum = 1L;
        for (List<Integer> cutYs : results.values()) {
            checksum = 31L * checksum + cutYs.hashCode();
        }
        return checksum;
    }

    private void writePieces(
            WebtoonPage page, BufferedImage image, List<Integer> cutYs) throws IOException {
        if (currentOutputDirectory == null) {
            throw new IllegalStateException("Page는 Episode Composite 안에서 방문해야 합니다.");
        }

        String fileName = page.path().getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String baseName = dot < 0 ? fileName : fileName.substring(0, dot);

        for (int i = 0; i < cutYs.size() - 1; i++) {
            int top = cutYs.get(i);
            int bottom = cutYs.get(i + 1);
            BufferedImage piece = image.getSubimage(
                    0, top, image.getWidth(), bottom - top);
            Path outputFile = currentOutputDirectory.resolve(
                    baseName + "_cut_" + i + ".jpg");

            if (!ImageIO.write(piece, "jpg", outputFile.toFile())) {
                throw new IOException("JPEG writer를 찾을 수 없습니다.");
            }
        }
        removeStalePieces(currentOutputDirectory, baseName, cutYs.size() - 1);
    }

    private static void removeStalePieces(
            Path outputDirectory, String baseName, int pieceCount) throws IOException {
        String prefix = baseName + "_cut_";
        File[] oldPieces = outputDirectory.toFile().listFiles((directory, name) ->
                name.startsWith(prefix)
                        && name.toLowerCase(Locale.ROOT).endsWith(".jpg"));
        if (oldPieces == null) {
            return;
        }

        for (File oldPiece : oldPieces) {
            String name = oldPiece.getName();
            String indexText = name.substring(prefix.length(), name.length() - 4);
            try {
                int index = Integer.parseInt(indexText);
                if (index >= pieceCount) {
                    Files.delete(oldPiece.toPath());
                }
            } catch (NumberFormatException ignored) {
                // 이 모듈이 만든 이름 형식이 아닌 파일은 건드리지 않는다.
            }
        }
    }
}

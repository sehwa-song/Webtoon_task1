package task2.variants.visitor;

import java.awt.Color;
import java.awt.Graphics2D;
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
import java.util.Objects;
import javax.imageio.ImageIO;

/** Page를 방문해 콘텐츠 구간을 탐색하고 선택적으로 JPEG로 저장하는 Concrete Visitor. */
public final class ExtractPageVisitor implements WebtoonVisitor {
    private final Path outputRoot;
    private final boolean writeFiles;
    private final boolean printResults;
    private final Map<Path, List<ContentRegion>> results =
            new LinkedHashMap<Path, List<ContentRegion>>();

    private Path currentOutputDirectory;

    public ExtractPageVisitor(
            Path outputRoot, boolean writeFiles, boolean printResults) {
        this.outputRoot = Objects.requireNonNull(outputRoot, "outputRoot")
                .toAbsolutePath().normalize();
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
        List<ContentRegion> regions = Collections.unmodifiableList(
                new ArrayList<ContentRegion>(
                        VisitorContentAlgorithm.findContentRegions(image)));
        results.put(page.path(), regions);

        if (printResults) {
            System.out.println(page.path() + " -> " + regions);
        }
        if (writeFiles) {
            writeRegions(page, image, regions);
        }
    }

    @Override
    public void leaveEpisode(WebtoonEpisode episode) {
        currentOutputDirectory = null;
    }

    @Override
    public void leaveBatch(WebtoonBatch batch) {
        // 전체 Batch 단위 후처리가 필요하면 이 위치에 추가할 수 있다.
    }

    public Map<Path, List<ContentRegion>> results() {
        return Collections.unmodifiableMap(
                new LinkedHashMap<Path, List<ContentRegion>>(results));
    }

    public long checksum() {
        long checksum = 1L;
        for (List<ContentRegion> regions : results.values()) {
            checksum = 31L * checksum + regions.hashCode();
        }
        return checksum;
    }

    private void writeRegions(
            WebtoonPage page,
            BufferedImage image,
            List<ContentRegion> regions) throws IOException {
        if (currentOutputDirectory == null) {
            throw new IllegalStateException(
                    "Page는 Episode Composite 안에서 방문해야 합니다.");
        }

        String fileName = page.path().getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String baseName = dot < 0 ? fileName : fileName.substring(0, dot);

        for (int index = 0; index < regions.size(); index++) {
            ContentRegion region = regions.get(index);
            BufferedImage piece = copyAsRgb(image, region);
            Path destination = currentOutputDirectory.resolve(
                    baseName + "_content_" + index + ".jpg");
            if (!ImageIO.write(piece, "jpg", destination.toFile())) {
                throw new IOException("JPEG writer를 찾을 수 없습니다: " + destination);
            }
        }
        removeStaleOutputs(currentOutputDirectory, baseName, regions.size());
    }

    private static BufferedImage copyAsRgb(
            BufferedImage source, ContentRegion region) {
        BufferedImage result = new BufferedImage(
                source.getWidth(), region.height(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, result.getWidth(), result.getHeight());
            graphics.drawImage(
                    source,
                    0,
                    0,
                    source.getWidth(),
                    region.height(),
                    0,
                    region.top(),
                    source.getWidth(),
                    region.bottomExclusive(),
                    null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private static void removeStaleOutputs(
            Path directory, String baseName, int regionCount) throws IOException {
        String prefix = baseName + "_content_";
        File[] oldFiles = directory.toFile().listFiles((ignored, name) ->
                name.startsWith(prefix)
                        && name.toLowerCase(Locale.ROOT).endsWith(".jpg"));
        if (oldFiles == null) {
            return;
        }

        for (File oldFile : oldFiles) {
            String name = oldFile.getName();
            String number = name.substring(prefix.length(), name.length() - 4);
            try {
                int index = Integer.parseInt(number);
                if (index >= regionCount) {
                    Files.delete(oldFile.toPath());
                }
            } catch (NumberFormatException ignored) {
                // 이 프로그램의 숫자 접미사 형식이 아닌 파일은 건드리지 않는다.
            }
        }
    }
}

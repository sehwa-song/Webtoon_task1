package task2.variants.visitor;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;

/** Task2 Visitor + Composite 변형을 독립 실행하는 CLI와 공개 API이다. */
public final class VisitorExtractorApp {
    private static final Path DEFAULT_OUTPUT = Paths.get("output-task2-visitor");

    private VisitorExtractorApp() {
    }

    public static void main(String[] args) {
        ImageIO.setUseCache(false);
        try {
            run(args);
        } catch (IllegalArgumentException | IOException failure) {
            System.err.println("Task2 Visitor 추출 실패: " + failure.getMessage());
        }
    }

    /** 단일 메모리 이미지도 반드시 Batch -> Episode -> Page 방문 경로로 처리한다. */
    public static List<ContentRegion> findContentRegions(BufferedImage image) {
        Objects.requireNonNull(image, "image");
        Path path = Paths.get("visitor-memory", "page-0.png");
        Map<Path, BufferedImage> images =
                new LinkedHashMap<Path, BufferedImage>();
        images.put(path, image);
        return analyzeLoadedImages(images).values().iterator().next();
    }

    /** 파일 I/O를 제외한 패턴 비용 비교용 API이다. */
    public static Map<Path, List<ContentRegion>> analyzeLoadedImages(
            Map<Path, BufferedImage> images) {
        Objects.requireNonNull(images, "images");
        if (images.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Map.Entry<Path, BufferedImage>> entries =
                new ArrayList<Map.Entry<Path, BufferedImage>>(images.entrySet());
        Collections.sort(entries, Comparator.comparing(
                entry -> entry.getKey().toString()));

        List<WebtoonPage> pages = new ArrayList<WebtoonPage>();
        for (Map.Entry<Path, BufferedImage> entry : entries) {
            pages.add(WebtoonPage.fromLoadedImage(entry.getKey(), entry.getValue()));
        }

        try {
            return analyzeBatch(buildBatch(pages));
        } catch (IOException impossibleForLoadedImages) {
            throw new IllegalStateException(
                    "메모리 이미지 처리 중 예상하지 못한 I/O 오류",
                    impossibleForLoadedImages);
        }
    }

    /** 실제 경로들을 Composite로 묶고 Visitor로 분석한다. */
    public static Map<Path, List<ContentRegion>> analyzePaths(List<Path> paths)
            throws IOException {
        Objects.requireNonNull(paths, "paths");
        if (paths.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Path> sortedPaths = new ArrayList<Path>(paths);
        Collections.sort(sortedPaths, Comparator.comparing(Path::toString));
        List<WebtoonPage> pages = new ArrayList<WebtoonPage>();
        for (Path path : sortedPaths) {
            pages.add(WebtoonPage.fromPath(path));
        }
        return analyzeBatch(buildBatch(pages));
    }

    /** JMH에서 결과가 제거되지 않도록 checksum을 돌려주는 일괄 작업 API이다. */
    public static long benchmarkLoadedImages(
            List<BufferedImage> images, int repetitions) {
        Objects.requireNonNull(images, "images");
        if (images.isEmpty()) {
            throw new IllegalArgumentException("images는 비어 있을 수 없습니다.");
        }
        if (repetitions <= 0) {
            throw new IllegalArgumentException("repetitions는 1 이상이어야 합니다.");
        }

        List<WebtoonPage> pages = new ArrayList<WebtoonPage>();
        for (int index = 0; index < images.size(); index++) {
            pages.add(WebtoonPage.fromLoadedImage(
                    Paths.get("visitor-benchmark", "page-" + index + ".png"),
                    images.get(index)));
        }
        WebtoonBatch batch = buildBatch(pages);

        long checksum = 1L;
        for (int repetition = 0; repetition < repetitions; repetition++) {
            ExtractPageVisitor visitor =
                    new ExtractPageVisitor(DEFAULT_OUTPUT, false, false);
            try {
                batch.accept(visitor);
            } catch (IOException impossibleForLoadedImages) {
                throw new IllegalStateException(
                        "메모리 이미지 처리 중 예상하지 못한 I/O 오류",
                        impossibleForLoadedImages);
            }
            checksum = 31L * checksum + visitor.checksum();
        }
        return checksum;
    }

    private static void run(String[] args) throws IOException {
        Options options = Options.parse(args);
        List<File> imageFiles = new ArrayList<File>();
        for (File input : options.inputs) {
            collectImageFiles(input, imageFiles);
        }
        Collections.sort(imageFiles, Comparator.comparing(File::getPath));

        if (imageFiles.isEmpty()) {
            System.err.println("처리할 이미지가 없습니다: " + options.inputs);
            return;
        }

        List<WebtoonPage> pages = new ArrayList<WebtoonPage>();
        for (File imageFile : imageFiles) {
            pages.add(WebtoonPage.fromPath(imageFile.toPath()));
        }

        WebtoonBatch batch = buildBatch(pages);
        ExtractPageVisitor visitor = new ExtractPageVisitor(
                options.outputRoot, !options.dryRun, true);
        batch.accept(visitor);
    }

    private static Map<Path, List<ContentRegion>> analyzeBatch(
            WebtoonBatch batch) throws IOException {
        ExtractPageVisitor visitor =
                new ExtractPageVisitor(DEFAULT_OUTPUT, false, false);
        batch.accept(visitor);
        return visitor.results();
    }

    private static WebtoonBatch buildBatch(List<WebtoonPage> pages) {
        Map<Path, List<WebtoonPage>> pagesByDirectory =
                new LinkedHashMap<Path, List<WebtoonPage>>();
        for (WebtoonPage page : pages) {
            Path parent = page.path().getParent();
            Path group = parent == null ? Paths.get("root") : parent;
            List<WebtoonPage> grouped = pagesByDirectory.get(group);
            if (grouped == null) {
                grouped = new ArrayList<WebtoonPage>();
                pagesByDirectory.put(group, grouped);
            }
            grouped.add(page);
        }

        List<WebtoonEpisode> episodes = new ArrayList<WebtoonEpisode>();
        for (Map.Entry<Path, List<WebtoonPage>> entry
                : pagesByDirectory.entrySet()) {
            Path directory = entry.getKey();
            Path name = directory.getFileName();
            String episodeName = name == null ? "root" : name.toString();
            episodes.add(new WebtoonEpisode(episodeName, entry.getValue()));
        }
        return new WebtoonBatch(episodes);
    }

    private static void collectImageFiles(File input, List<File> result) {
        if (input.isFile()) {
            if (isSupportedImage(input)) {
                result.add(input);
            }
            return;
        }
        File[] children = input.listFiles();
        if (children == null) {
            return;
        }
        Arrays.sort(children, Comparator.comparing(File::getName));
        for (File child : children) {
            collectImageFiles(child, result);
        }
    }

    private static boolean isSupportedImage(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".png");
    }

    private static final class Options {
        final boolean dryRun;
        final Path outputRoot;
        final List<File> inputs;

        Options(boolean dryRun, Path outputRoot, List<File> inputs) {
            this.dryRun = dryRun;
            this.outputRoot = outputRoot;
            this.inputs = inputs;
        }

        static Options parse(String[] args) {
            boolean dryRun = false;
            Path outputRoot = DEFAULT_OUTPUT;
            List<File> inputs = new ArrayList<File>();

            for (String arg : args) {
                if ("--dry-run".equals(arg)) {
                    dryRun = true;
                } else if (arg.startsWith("--output=")) {
                    String value = arg.substring("--output=".length());
                    if (value.isEmpty()) {
                        throw new IllegalArgumentException(
                                "--output 경로가 비어 있습니다.");
                    }
                    outputRoot = Paths.get(value);
                } else if (arg.startsWith("--")) {
                    throw new IllegalArgumentException("알 수 없는 옵션: " + arg);
                } else {
                    inputs.add(new File(arg));
                }
            }
            if (inputs.isEmpty()) {
                inputs.add(new File("images"));
            }
            return new Options(
                    dryRun,
                    outputRoot,
                    Collections.unmodifiableList(new ArrayList<File>(inputs)));
        }
    }
}

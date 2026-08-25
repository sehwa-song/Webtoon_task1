package variants.visitor;

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

/**
 * 독립 실행 가능한 Visitor + Composite 웹툰 분할 실험군이다.
 *
 * <p>CLI 예:</p>
 * <pre>
 * java variants.visitor.VisitorSplitterApp --dry-run images/002/005.jpg
 * java variants.visitor.VisitorSplitterApp --output=output-visitor images
 * </pre>
 */
public final class VisitorSplitterApp {
    private static final Path DEFAULT_OUTPUT = Paths.get("output-visitor");

    private VisitorSplitterApp() {
    }

    public static void main(String[] args) {
        try {
            run(args);
        } catch (IllegalArgumentException | IOException e) {
            System.err.println("Visitor 분할 실행 실패: " + e.getMessage());
        }
    }

    /**
     * 단일 이미지 알고리즘 벤치마크용 API다.
     * 직접 알고리즘을 호출하지 않고 Batch -> Episode -> Page accept/visit를 거친다.
     */
    public static List<Integer> findCutPoints(BufferedImage image) {
        Objects.requireNonNull(image, "image");
        Path path = Paths.get("visitor-memory", "page-0.png");
        Map<Path, BufferedImage> input = new LinkedHashMap<>();
        input.put(path, image);
        return analyzeLoadedImages(input).values().iterator().next();
    }

    /**
     * 미리 디코딩한 이미지로 I/O를 제외한 패턴 비용을 비교하는 배치 API다.
     * 반환 Map의 경로는 절대 경로로 정규화된다.
     */
    public static Map<Path, List<Integer>> analyzeLoadedImages(
            Map<Path, BufferedImage> images) {
        Objects.requireNonNull(images, "images");
        if (images.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Map.Entry<Path, BufferedImage>> entries = new ArrayList<>(images.entrySet());
        entries.sort(Comparator.comparing(entry -> entry.getKey().toString()));

        List<WebtoonPage> pages = new ArrayList<>();
        for (Map.Entry<Path, BufferedImage> entry : entries) {
            pages.add(WebtoonPage.fromLoadedImage(entry.getKey(), entry.getValue()));
        }

        try {
            return analyzeBatch(buildBatch(pages));
        } catch (IOException impossibleForLoadedImages) {
            throw new IllegalStateException(
                    "메모리 이미지 Visitor 분석 중 예상하지 못한 I/O 오류",
                    impossibleForLoadedImages);
        }
    }

    /** 파일 로드를 포함한 Visitor 경로를 벤치마크하거나 검증할 때 사용한다. */
    public static Map<Path, List<Integer>> analyzePaths(List<Path> paths)
            throws IOException {
        Objects.requireNonNull(paths, "paths");
        if (paths.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Path> sortedPaths = new ArrayList<>(paths);
        sortedPaths.sort(Comparator.comparing(Path::toString));
        List<WebtoonPage> pages = new ArrayList<>();
        for (Path path : sortedPaths) {
            pages.add(WebtoonPage.fromPath(path));
        }
        return analyzeBatch(buildBatch(pages));
    }

    /**
     * Composite를 한 번 구성한 뒤 매 반복마다 새 Concrete Visitor로 순회한다.
     * 반환 checksum은 실제 visitPage 결과만으로 계산된다.
     */
    public static long benchmarkLoadedImages(
            List<BufferedImage> images, int repetitions) {
        Objects.requireNonNull(images, "images");
        if (images.isEmpty()) {
            throw new IllegalArgumentException("images는 비어 있을 수 없습니다.");
        }
        if (repetitions <= 0) {
            throw new IllegalArgumentException("repetitions는 1 이상이어야 합니다.");
        }

        List<WebtoonPage> pages = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            Path path = Paths.get("visitor-benchmark", "page-" + i + ".png");
            pages.add(WebtoonPage.fromLoadedImage(path, images.get(i)));
        }
        WebtoonBatch batch = buildBatch(pages);

        long checksum = 1L;
        for (int repetition = 0; repetition < repetitions; repetition++) {
            SplitPageVisitor visitor = new SplitPageVisitor(DEFAULT_OUTPUT, false, false);
            try {
                batch.accept(visitor);
            } catch (IOException impossibleForLoadedImages) {
                throw new IllegalStateException(
                        "메모리 이미지 Visitor 벤치마크 중 예상하지 못한 I/O 오류",
                        impossibleForLoadedImages);
            }
            checksum = 31L * checksum + visitor.checksum();
        }
        return checksum;
    }

    private static void run(String[] args) throws IOException {
        Options options = Options.parse(args);
        List<File> imageFiles = new ArrayList<>();
        for (File input : options.inputs) {
            collectImageFiles(input, imageFiles);
        }
        imageFiles.sort(Comparator.comparing(File::getPath));

        if (imageFiles.isEmpty()) {
            System.err.println("처리할 이미지가 없습니다.");
            return;
        }

        List<WebtoonPage> pages = new ArrayList<>();
        for (File imageFile : imageFiles) {
            pages.add(WebtoonPage.fromPath(imageFile.toPath()));
        }

        WebtoonBatch batch = buildBatch(pages);
        SplitPageVisitor visitor = new SplitPageVisitor(
                options.outputRoot, !options.dryRun, true);
        batch.accept(visitor);
    }

    private static Map<Path, List<Integer>> analyzeBatch(WebtoonBatch batch)
            throws IOException {
        SplitPageVisitor visitor = new SplitPageVisitor(DEFAULT_OUTPUT, false, false);
        batch.accept(visitor);
        return visitor.results();
    }

    private static WebtoonBatch buildBatch(List<WebtoonPage> pages) {
        Map<Path, List<WebtoonPage>> pagesByDirectory = new LinkedHashMap<>();
        for (WebtoonPage page : pages) {
            Path parent = page.path().getParent();
            Path groupKey = parent == null ? Paths.get("root") : parent;
            pagesByDirectory.computeIfAbsent(groupKey, ignored -> new ArrayList<>())
                    .add(page);
        }

        List<WebtoonEpisode> episodes = new ArrayList<>();
        for (Map.Entry<Path, List<WebtoonPage>> entry : pagesByDirectory.entrySet()) {
            Path directory = entry.getKey();
            Path fileName = directory.getFileName();
            String episodeName = fileName == null ? "root" : fileName.toString();
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
            List<File> inputs = new ArrayList<>();

            for (String arg : args) {
                if ("--dry-run".equals(arg)) {
                    dryRun = true;
                } else if (arg.startsWith("--output=")) {
                    String value = arg.substring("--output=".length());
                    if (value.isEmpty()) {
                        throw new IllegalArgumentException("--output 경로가 비어 있습니다.");
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
                    Collections.unmodifiableList(new ArrayList<>(inputs)));
        }
    }
}

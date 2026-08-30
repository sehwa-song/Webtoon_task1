package task1.visitor;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Batch → Episode → Page 계층을 만든 뒤 batch.accept(visitor)로 처리합니다. */
public final class VisitorSplitterApp {
    private VisitorSplitterApp() {
    }

    public static void main(String[] args) {
        long programStart = System.nanoTime();
        boolean dryRun = false;
        File outputRoot = new File("output-task1-patterns/visitor");
        List<File> inputs = new ArrayList<>();

        for (String arg : args) {
            if ("--dry-run".equals(arg)) {
                dryRun = true;
            } else if (arg.startsWith("--output=")) {
                outputRoot = new File(arg.substring("--output=".length()));
            } else if ("--help".equals(arg)) {
                printHelp();
                return;
            } else {
                inputs.add(new File(arg));
            }
        }
        if (inputs.isEmpty()) {
            inputs.add(new File("images"));
        }

        List<File> imageFiles = new ArrayList<>();
        for (File input : inputs) {
            collectImages(input, imageFiles);
        }
        imageFiles.sort(Comparator.comparing(File::getPath));

        WebtoonBatch batch = buildComposite(imageFiles);
        SplitPageVisitor visitor = new SplitPageVisitor(outputRoot, dryRun);

        // 이 한 줄에서 Composite 전체가 순회되고 각 Page에 Visitor 작업이 적용됩니다.
        batch.accept(visitor);

        visitor.printSummary(System.nanoTime() - programStart);
    }

    private static WebtoonBatch buildComposite(List<File> files) {
        WebtoonBatch batch = new WebtoonBatch("task1-images");
        Map<String, WebtoonEpisode> episodes = new LinkedHashMap<>();

        for (File file : files) {
            File parent = file.getParentFile();
            String episodeName = parent == null ? "root" : parent.getName();
            WebtoonEpisode episode = episodes.computeIfAbsent(
                    episodeName,
                    WebtoonEpisode::new);
            episode.add(new WebtoonPage(file));
        }
        for (WebtoonEpisode episode : episodes.values()) {
            batch.add(episode);
        }
        return batch;
    }

    private static void collectImages(File input, List<File> result) {
        if (input.isFile()) {
            if (isImage(input)) {
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
            collectImages(child, result);
        }
    }

    private static boolean isImage(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".png");
    }

    private static void printHelp() {
        System.out.println("java ... VisitorSplitterApp [옵션] [입력 경로 ...]");
        System.out.println("  --dry-run      파일 저장 생략");
        System.out.println("  --output=경로  출력 폴더 변경");
        System.out.println("입력 생략 시 images 폴더 전체를 정렬해 처리합니다.");
    }
}

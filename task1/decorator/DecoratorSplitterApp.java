package task1.decorator;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/** Decorator 패턴 버전의 독립 실행 진입점입니다. */
public final class DecoratorSplitterApp {
    private DecoratorSplitterApp() {
    }

    public static void main(String[] args) {
        long programStart = System.nanoTime();
        Options options = Options.parse(args);
        if (options.help) {
            printHelp();
            return;
        }

        /*
         * 감싸는 순서(안쪽 -> 바깥쪽):
         * RGB 원본 탐색기 -> 결과 검증 Decorator -> 시간 측정 Decorator
         * 따라서 Timing은 원본 탐색과 검증에 걸린 시간을 함께 측정합니다.
         */
        CutPointDetector core = new RgbMarginCutPointDetector();
        CutPointDetector validated = new CutPointValidationDecorator(core);
        TimingCutPointDetectorDecorator timing =
                new TimingCutPointDetectorDecorator(validated);
        CutPointDetector detector = timing;

        List<File> images = collectInputs(options.inputs);
        if (images.isEmpty()) {
            System.err.println("처리할 이미지가 없습니다.");
            return;
        }

        JpegPieceWriter writer = new JpegPieceWriter();
        long totalLoad = 0;
        long totalDetect = 0;
        long totalWrite = 0;
        int successes = 0;
        int failures = 0;
        int pieces = 0;

        System.out.println("Decorator 조합: " + detector.name());
        System.out.println("이미지 수: " + images.size());
        System.out.println("저장 여부: " + (options.dryRun ? "아니오(--dry-run)" : "예"));

        for (File file : images) {
            long imageStart = System.nanoTime();
            try {
                long start = System.nanoTime();
                BufferedImage image = ImageIO.read(file);
                long load = System.nanoTime() - start;
                if (image == null) {
                    throw new IOException("지원하지 않거나 손상된 이미지입니다.");
                }

                start = System.nanoTime();
                List<Integer> points = detector.findCutPoints(image);
                long detect = System.nanoTime() - start;

                long write = 0;
                int imagePieces = points.size() - 1;
                if (!options.dryRun) {
                    start = System.nanoTime();
                    imagePieces = writer.write(file, image, points, options.outputRoot);
                    write = System.nanoTime() - start;
                }

                long total = System.nanoTime() - imageStart;
                totalLoad += load;
                totalDetect += detect;
                totalWrite += write;
                pieces += imagePieces;
                successes++;
                printImage(file, points, load, detect, write, total, options.dryRun);
            } catch (IOException | RuntimeException exception) {
                failures++;
                System.err.println("처리 실패: " + file + " (" + exception.getMessage() + ")");
            }
        }

        System.out.println("\n========== Decorator 전체 결과 ==========");
        System.out.println("성공 / 실패     : " + successes + " / " + failures);
        System.out.println("결과 컷 수       : " + pieces);
        printTime("누적 load", totalLoad);
        printTime("누적 detect", totalDetect);
        printTime("누적 write", totalWrite);
        printTime("Decorator 내부 측정", timing.getTotalNanos());
        System.out.println("Decorator 호출 수 : " + timing.getCallCount());
        printTime("프로그램 전체", System.nanoTime() - programStart);
        if (!options.dryRun) {
            System.out.println("출력 폴더        : " + options.outputRoot.getPath());
        }
    }

    private static void printImage(File file, List<Integer> points,
            long load, long detect, long write, long total, boolean dryRun) {
        System.out.println("\n처리 완료: " + file.getPath());
        System.out.println("  컷 위치: " + points);
        System.out.println("  컷 개수: " + (points.size() - 1));
        printTime("  load", load);
        printTime("  detect", detect);
        if (!dryRun) {
            printTime("  write", write);
        }
        printTime("  total", total);
    }

    private static void printTime(String label, long nanos) {
        System.out.printf(Locale.ROOT, "%-22s: %,.3f ms%n",
                label, nanos / 1_000_000.0);
    }

    private static List<File> collectInputs(List<File> requested) {
        List<File> inputs = requested.isEmpty()
                ? List.of(new File("images")) : requested;
        List<File> result = new ArrayList<>();
        for (File input : inputs) {
            collectRecursively(input, result);
        }
        result.sort(Comparator.comparing(File::getPath));
        return result;
    }

    private static void collectRecursively(File file, List<File> result) {
        if (file.isFile()) {
            if (isImage(file)) {
                result.add(file);
            }
            return;
        }
        File[] children = file.listFiles();
        if (children == null) {
            return;
        }
        Arrays.sort(children, Comparator.comparing(File::getName));
        for (File child : children) {
            collectRecursively(child, result);
        }
    }

    private static boolean isImage(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png");
    }

    private static void printHelp() {
        System.out.println("사용법: java ...DecoratorSplitterApp [옵션] [파일/폴더 ...]");
        System.out.println("  --dry-run       파일을 저장하지 않고 분석만 실행");
        System.out.println("  --output=폴더   출력 폴더 변경");
        System.out.println("입력을 생략하면 images 폴더 전체를 처리합니다.");
    }

    private static final class Options {
        private boolean dryRun;
        private boolean help;
        private File outputRoot = new File("output-task1-patterns/decorator");
        private final List<File> inputs = new ArrayList<>();

        private static Options parse(String[] args) {
            Options options = new Options();
            for (String arg : args) {
                if ("--dry-run".equals(arg)) {
                    options.dryRun = true;
                } else if ("--help".equals(arg)) {
                    options.help = true;
                } else if (arg.startsWith("--output=")) {
                    options.outputRoot = new File(arg.substring("--output=".length()));
                } else {
                    options.inputs.add(new File(arg));
                }
            }
            return options;
        }
    }
}

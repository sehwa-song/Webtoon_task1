package task1.proxy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Proxy 패턴 버전의 독립 실행 진입점입니다. */
public final class ProxySplitterApp {
    private ProxySplitterApp() {
    }

    public static void main(String[] args) {
        long programStart = System.nanoTime();
        Options options = Options.parse(args);
        if (options.help) {
            printHelp();
            return;
        }

        List<File> images = collectInputs(options.inputs);
        if (images.isEmpty()) {
            System.err.println("처리할 이미지가 없습니다.");
            return;
        }

        // Real Subject를 Proxy로 감싼 뒤, 서비스에는 인터페이스 타입으로 전달합니다.
        ImageRepository realSubject = new DiskImageRepository();
        CachingImageRepositoryProxy proxy =
                new CachingImageRepositoryProxy(realSubject);
        ImageRepository repositoryUsedByService = proxy;

        ProxySplittingService service = new ProxySplittingService(
                repositoryUsedByService,
                new RgbMarginCutPointDetector(),
                new JpegPieceWriter(),
                options.outputRoot,
                options.dryRun);

        long totalLoad = 0;
        long totalDetect = 0;
        long totalWrite = 0;
        int successes = 0;
        int failures = 0;
        int pieces = 0;

        System.out.println("Repository: CachingImageRepositoryProxy -> DiskImageRepository");
        System.out.println("고유 이미지 수: " + images.size());
        System.out.println("반복 횟수: " + options.passes);
        System.out.println("저장 여부: " + (options.dryRun ? "아니오(--dry-run)" : "예"));

        for (int pass = 1; pass <= options.passes; pass++) {
            if (options.passes > 1) {
                System.out.println("\n---------- pass " + pass + " ----------");
            }
            for (File file : images) {
                try {
                    ProxyProcessingResult result = service.process(file);
                    totalLoad += result.getLoadNanos();
                    totalDetect += result.getDetectNanos();
                    totalWrite += result.getWriteNanos();
                    pieces += result.getCutPoints().size() - 1;
                    successes++;
                    printImage(file, result, options.dryRun);
                } catch (IOException | RuntimeException exception) {
                    failures++;
                    System.err.println("처리 실패: " + file + " ("
                            + exception.getMessage() + ")");
                }
            }
        }

        System.out.println("\n========== Proxy 전체 결과 ==========");
        System.out.println("성공 / 실패       : " + successes + " / " + failures);
        System.out.println("결과 컷 수         : " + pieces);
        System.out.println("캐시 hit / miss    : "
                + proxy.getHitCount() + " / " + proxy.getMissCount());
        System.out.println("캐시 이미지 수     : " + proxy.getCachedImageCount());
        printTime("누적 load", totalLoad);
        printTime("누적 detect", totalDetect);
        printTime("누적 write", totalWrite);
        printTime("프로그램 전체", System.nanoTime() - programStart);
        if (!options.dryRun) {
            System.out.println("출력 폴더          : " + options.outputRoot.getPath());
        }
    }

    private static void printImage(File file, ProxyProcessingResult result,
            boolean dryRun) {
        System.out.println("\n처리 완료: " + file.getPath());
        System.out.println("  컷 위치: " + result.getCutPoints());
        System.out.println("  컷 개수: " + (result.getCutPoints().size() - 1));
        printTime("  load", result.getLoadNanos());
        printTime("  detect", result.getDetectNanos());
        if (!dryRun) {
            printTime("  write", result.getWriteNanos());
        }
        printTime("  total", result.getTotalNanos());
    }

    private static void printTime(String label, long nanos) {
        System.out.printf(Locale.ROOT, "%-20s: %,.3f ms%n",
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
        System.out.println("사용법: java ...ProxySplitterApp [옵션] [파일/폴더 ...]");
        System.out.println("  --dry-run       파일을 저장하지 않고 분석만 실행");
        System.out.println("  --output=폴더   출력 폴더 변경");
        System.out.println("  --passes=N      같은 입력을 N회 읽어 캐시 hit 확인(기본 1)");
        System.out.println("입력을 생략하면 images 폴더 전체를 처리합니다.");
    }

    private static final class Options {
        private boolean dryRun;
        private boolean help;
        private int passes = 1;
        private File outputRoot = new File("output-task1-patterns/proxy");
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
                } else if (arg.startsWith("--passes=")) {
                    options.passes = parsePasses(arg.substring("--passes=".length()));
                } else {
                    options.inputs.add(new File(arg));
                }
            }
            return options;
        }

        private static int parsePasses(String text) {
            try {
                int value = Integer.parseInt(text);
                if (value < 1) {
                    throw new IllegalArgumentException("--passes는 1 이상이어야 합니다.");
                }
                return value;
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("잘못된 --passes 값: " + text);
            }
        }
    }
}

package task1.factory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 실행 시 --cached 유무만으로 제품군 전체를 교체하는 Abstract Factory 예제입니다.
 */
public final class FactorySplitterApp {
    private FactorySplitterApp() {
    }

    public static void main(String[] args) {
        long programStart = System.nanoTime();
        boolean cached = false;
        boolean dryRun = false;
        File outputRoot = new File("output-task1-patterns/factory");
        List<File> inputs = new ArrayList<>();

        for (String arg : args) {
            if ("--cached".equals(arg)) {
                cached = true;
            } else if ("--dry-run".equals(arg)) {
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

        List<File> images = new ArrayList<>();
        for (File input : inputs) {
            collectImages(input, images);
        }
        images.sort(Comparator.comparing(File::getPath));

        ProcessingFamilyFactory factory = cached
                ? new CachedProcessingFactory()
                : new StandardProcessingFactory();
        WebtoonProcessingService service =
                new WebtoonProcessingService(factory);

        long loadTotal = 0;
        long detectTotal = 0;
        long writeTotal = 0;
        int pieces = 0;
        int success = 0;
        int failure = 0;

        System.out.println("팩토리: " + factory.name());
        System.out.println("제품군: " + service.products());
        System.out.println("입력 이미지: " + images.size());

        for (File image : images) {
            try {
                ProcessingResult result =
                        service.process(image, outputRoot, dryRun);
                loadTotal += result.loadNanos;
                detectTotal += result.detectNanos;
                writeTotal += result.writeNanos;
                pieces += result.pieceCount();
                success++;

                System.out.println("\n처리 완료: " + image.getPath());
                System.out.println("  컷 위치: " + result.cutPoints);
                System.out.println("  결과 컷: " + result.pieceCount());
                printTime("  로딩", result.loadNanos);
                printTime("  탐색", result.detectNanos);
                if (!dryRun) {
                    printTime("  저장", result.writeNanos);
                }
                printTime("  이미지 전체", result.totalNanos);
            } catch (IOException | RuntimeException exception) {
                failure++;
                System.err.println("처리 실패: " + image + " ("
                        + exception.getMessage() + ")");
            }
        }

        System.out.println("\n========== Abstract Factory 실행 요약 ==========");
        System.out.println("팩토리              : " + factory.name());
        System.out.println("성공 / 실패         : " + success + " / " + failure);
        System.out.println("전체 결과 컷        : " + pieces);
        printTime("누적 로딩", loadTotal);
        printTime("누적 탐색", detectTotal);
        printTime(dryRun ? "누적 저장(생략)" : "누적 저장", writeTotal);
        printTime("프로그램 전체", System.nanoTime() - programStart);
        if (!dryRun) {
            System.out.println("출력 폴더           : " + outputRoot.getPath());
        }
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

    private static void printTime(String label, long nanos) {
        System.out.printf(
                Locale.ROOT, "%-20s: %,.3f ms%n", label, nanos / 1_000_000.0);
    }

    private static void printHelp() {
        System.out.println("java ... FactorySplitterApp [옵션] [입력 경로 ...]");
        System.out.println("  --cached       캐시 제품군 선택");
        System.out.println("  --dry-run      파일 저장 생략");
        System.out.println("  --output=경로  출력 폴더 변경");
        System.out.println("입력 생략 시 images 폴더 전체를 정렬해 처리합니다.");
    }
}

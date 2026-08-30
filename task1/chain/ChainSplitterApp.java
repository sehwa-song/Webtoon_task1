package task1.chain;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Chain of Responsibility 버전을 독립적으로 실행하는 진입점입니다. */
public final class ChainSplitterApp {
    private ChainSplitterApp() {
    }

    public static void main(String[] args) {
        long wholeStart = System.nanoTime();

        boolean dryRun = false;
        File outputRoot = new File("output-task1-patterns/chain");
        List<File> inputs = new ArrayList<>();

        for (String arg : args) {
            if ("--dry-run".equals(arg)) {
                dryRun = true;
            } else if (arg.startsWith("--output=")) {
                outputRoot = new File(
                        arg.substring("--output=".length()));
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
            collectImageFiles(input, imageFiles);
        }
        imageFiles.sort(Comparator.comparing(File::getPath));

        if (imageFiles.isEmpty()) {
            System.err.println("처리할 이미지 파일을 찾지 못했습니다.");
            return;
        }

        ChainWebtoonSplitter splitter = new ChainWebtoonSplitter(
                new ChainCutPointDetector());
        long loadTotal = 0L;
        long detectTotal = 0L;
        long writeTotal = 0L;
        int pieces = 0;
        int success = 0;
        int failure = 0;

        System.out.println("패턴: Chain of Responsibility");
        System.out.println("연쇄: 최소 높이 → 컬러 안정성 → 안전 cutY");
        System.out.println("입력 이미지 수: " + imageFiles.size());
        System.out.println("파일 저장: " + (dryRun ? "아니오(dry-run)" : "예"));

        for (File imageFile : imageFiles) {
            try {
                ChainSplitResult result = splitter.split(
                        imageFile, outputRoot, dryRun);
                success++;
                pieces += result.getPieceCount();
                loadTotal += result.getLoadNanos();
                detectTotal += result.getDetectNanos();
                writeTotal += result.getWriteNanos();

                System.out.println();
                System.out.println("처리 완료: " + imageFile.getPath());
                System.out.println("  컷 위치: " + result.getCutPoints());
                System.out.println("  결과 컷 수: " + result.getPieceCount());
                printTime("  load", result.getLoadNanos());
                printTime("  detect", result.getDetectNanos());
                printTime("  write", result.getWriteNanos());
                printTime("  total", result.getTotalNanos());
            } catch (IOException | RuntimeException exception) {
                failure++;
                System.err.println(
                        "처리 실패: " + imageFile.getPath()
                        + " (" + exception.getMessage() + ")");
            }
        }

        System.out.println();
        System.out.println("========== 책임 연쇄 전체 요약 ==========");
        System.out.println("성공/실패 이미지: " + success + " / " + failure);
        System.out.println("생성 예정/완료 컷: " + pieces);
        printTime("누적 load", loadTotal);
        printTime("누적 detect", detectTotal);
        printTime("누적 write", writeTotal);
        printTime("프로그램 전체", System.nanoTime() - wholeStart);
        if (!dryRun) {
            System.out.println("출력 폴더: " + outputRoot.getPath());
        }
    }

    private static void collectImageFiles(File input, List<File> result) {
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
            collectImageFiles(child, result);
        }
    }

    private static boolean isImage(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".png");
    }

    private static void printTime(String label, long nanos) {
        System.out.printf(
                Locale.ROOT,
                "%-17s: %,.3f ms%n",
                label,
                nanos / 1_000_000.0);
    }

    private static void printHelp() {
        System.out.println("Chain of Responsibility Task1 사용법");
        System.out.println(
                "  java ... [--dry-run] [--output=폴더] [입력 경로 ...]");
        System.out.println(
                "입력 경로가 없으면 images 폴더 전체를 처리합니다.");
    }
}

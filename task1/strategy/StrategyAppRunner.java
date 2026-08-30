package task1.strategy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * 각 Strategy 실행 파일이 함께 사용하는 입출력 실행기입니다.
 * 픽셀 알고리즘은 포함하지 않고, 선택된 Strategy를 호출하기만 합니다.
 */
public final class StrategyAppRunner {
    private StrategyAppRunner() {
    }

    public static void run(
            String[] args,
            RowClassifier classifier,
            File defaultOutputRoot) {

        // 파일 탐색과 정렬까지 포함한 실제 전체 경과 시간의 시작점입니다.
        long wholeRunStart = System.nanoTime();

        boolean dryRun = false;
        File outputRoot = defaultOutputRoot;
        List<File> requestedInputs = new ArrayList<>();

        for (String arg : args) {
            if ("--dry-run".equals(arg)) {
                dryRun = true;
            } else if (arg.startsWith("--output=")) {
                outputRoot = new File(
                        arg.substring("--output=".length()));
            } else if ("--help".equals(arg)) {
                printHelp(classifier);
                return;
            } else {
                requestedInputs.add(new File(arg));
            }
        }

        // 입력 경로를 생략하면 images 폴더 전체를 처리합니다.
        if (requestedInputs.isEmpty()) {
            requestedInputs.add(new File("images"));
        }

        List<File> imageFiles = new ArrayList<>();
        for (File input : requestedInputs) {
            collectImageFiles(input, imageFiles);
        }
        imageFiles.sort(Comparator.comparing(File::getPath));

        if (imageFiles.isEmpty()) {
            System.err.println("처리할 이미지 파일을 찾지 못했습니다.");
            return;
        }

        StrategySplitter splitter = new StrategySplitter(classifier);

        long accumulatedLoadNanos = 0;
        long accumulatedDetectNanos = 0;
        long accumulatedWriteNanos = 0;
        int successCount = 0;
        int failureCount = 0;
        int totalPieceCount = 0;

        System.out.println("행 분류 전략: " + classifier.name());
        System.out.println("입력 이미지 수: " + imageFiles.size());
        System.out.println("파일 저장: " + (dryRun ? "안 함(dry-run)" : "함"));

        for (File imageFile : imageFiles) {
            try {
                long fileStart = System.nanoTime();

                long loadStart = System.nanoTime();
                BufferedImage image = ImageIO.read(imageFile);
                long loadNanos = System.nanoTime() - loadStart;

                if (image == null) {
                    throw new IOException(
                            "지원하지 않거나 읽을 수 없는 이미지입니다.");
                }

                long detectStart = System.nanoTime();
                List<Integer> cutPoints =
                        splitter.findCutPoints(image);
                long detectNanos = System.nanoTime() - detectStart;

                long writeNanos = 0;
                if (!dryRun) {
                    long writeStart = System.nanoTime();
                    writePieces(
                            imageFile,
                            image,
                            cutPoints,
                            outputRoot);
                    writeNanos = System.nanoTime() - writeStart;
                }

                long fileNanos = System.nanoTime() - fileStart;
                int pieceCount = cutPoints.size() - 1;

                accumulatedLoadNanos += loadNanos;
                accumulatedDetectNanos += detectNanos;
                accumulatedWriteNanos += writeNanos;
                totalPieceCount += pieceCount;
                successCount++;

                printImageResult(
                        imageFile,
                        cutPoints,
                        loadNanos,
                        detectNanos,
                        writeNanos,
                        fileNanos,
                        dryRun);
            } catch (IOException | RuntimeException exception) {
                failureCount++;
                System.err.println(
                        "처리 실패: " + imageFile.getPath()
                        + " (" + exception.getMessage() + ")");
            }
        }

        long wholeRunNanos = System.nanoTime() - wholeRunStart;

        System.out.println();
        System.out.println("========== 실행 시간 요약 ==========");
        System.out.println("행 분류 전략       : " + classifier.name());
        System.out.println("성공/실패 이미지   : "
                + successCount + " / " + failureCount);
        System.out.println("생성 대상 컷 수    : " + totalPieceCount);
        printTime("누적 이미지 로딩", accumulatedLoadNanos);
        printTime("누적 컷 탐색", accumulatedDetectNanos);
        printTime(
                dryRun ? "누적 파일 저장(생략)" : "누적 파일 저장",
                accumulatedWriteNanos);
        printTime("프로그램 전체", wholeRunNanos);

        if (!dryRun) {
            System.out.println("출력 폴더          : "
                    + outputRoot.getPath());
        }
    }

    /** 벤치마크나 테스트에서 파일 저장 없이 전략만 호출할 때 사용할 수 있습니다. */
    public static List<Integer> findCutPoints(
            BufferedImage image,
            RowClassifier classifier) {

        return new StrategySplitter(classifier).findCutPoints(image);
    }

    private static void printImageResult(
            File imageFile,
            List<Integer> cutPoints,
            long loadNanos,
            long detectNanos,
            long writeNanos,
            long fileNanos,
            boolean dryRun) {

        System.out.println();
        System.out.println("처리 완료: " + imageFile.getPath());
        System.out.println("  컷 위치: " + cutPoints);
        System.out.println("  결과 컷 수: " + (cutPoints.size() - 1));
        printTime("  로딩", loadNanos);
        printTime("  컷 탐색", detectNanos);

        if (!dryRun) {
            printTime("  저장", writeNanos);
        }

        printTime("  이미지 전체", fileNanos);
    }

    private static void printTime(String label, long nanos) {
        double milliseconds = nanos / 1_000_000.0;
        System.out.printf(
                Locale.ROOT,
                "%-20s: %,.3f ms%n",
                label,
                milliseconds);
    }

    private static void collectImageFiles(
            File input,
            List<File> result) {

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

    private static void writePieces(
            File imageFile,
            BufferedImage image,
            List<Integer> cutPoints,
            File outputRoot) throws IOException {

        File parent = imageFile.getParentFile();
        String episodeName = parent == null ? "root" : parent.getName();
        File outputDirectory = new File(outputRoot, episodeName);

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

            File outputFile = new File(
                    outputDirectory,
                    baseName + "_cut_"
                    + String.format(Locale.ROOT, "%03d", i)
                    + ".jpg");

            if (!ImageIO.write(piece, "jpg", outputFile)) {
                throw new IOException("JPEG 저장기를 찾을 수 없습니다.");
            }
        }

        removeStalePieces(
                outputDirectory,
                baseName,
                cutPoints.size() - 1);
    }

    /**
     * PNG처럼 알파 채널이 있는 원본도 JPEG로 저장할 수 있도록
     * 잘라낼 범위를 흰 배경의 RGB 이미지로 복사합니다.
     */
    private static BufferedImage toRgbPiece(
            BufferedImage source,
            int top,
            int bottom) {

        int pieceHeight = bottom - top;
        BufferedImage piece = new BufferedImage(
                source.getWidth(),
                pieceHeight,
                BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = piece.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(
                    0,
                    0,
                    piece.getWidth(),
                    piece.getHeight());
            graphics.drawImage(
                    source,
                    0,
                    0,
                    source.getWidth(),
                    pieceHeight,
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
            File outputDirectory,
            String baseName,
            int currentPieceCount) throws IOException {

        String prefix = baseName + "_cut_";
        File[] oldPieces = outputDirectory.listFiles((directory, name) ->
                name.startsWith(prefix)
                && name.toLowerCase(Locale.ROOT).endsWith(".jpg"));

        if (oldPieces == null) {
            return;
        }

        for (File oldPiece : oldPieces) {
            String name = oldPiece.getName();
            String numberText = name.substring(
                    prefix.length(),
                    name.length() - ".jpg".length());

            try {
                int pieceNumber = Integer.parseInt(numberText);

                if (pieceNumber >= currentPieceCount
                        && !oldPiece.delete()) {
                    throw new IOException(
                            "이전 결과 파일을 삭제하지 못했습니다: "
                            + oldPiece.getPath());
                }
            } catch (NumberFormatException ignored) {
                // 현재 프로그램의 번호 형식이 아닌 파일은 건드리지 않습니다.
            }
        }
    }

    private static void printHelp(RowClassifier classifier) {
        System.out.println("행 분류 전략: " + classifier.name());
        System.out.println("사용법:");
        System.out.println("  java ... [--dry-run] [--output=폴더] [입력 경로 ...]");
        System.out.println("입력 경로를 생략하면 images 폴더 전체를 처리합니다.");
    }
}

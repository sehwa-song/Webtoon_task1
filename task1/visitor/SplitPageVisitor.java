package task1.visitor;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * Composite 구조를 방문하고, 실제 작업 대상인 WebtoonPage(Leaf)에만 컷 분할을 적용합니다.
 * 계층 클래스에는 픽셀 분석이나 파일 저장 코드가 들어가지 않습니다.
 */
public final class SplitPageVisitor implements WebtoonVisitor {
    private final File outputRoot;
    private final boolean dryRun;
    private final RgbMarginCutPointDetector detector =
            new RgbMarginCutPointDetector();
    private final JpegPieceWriter writer = new JpegPieceWriter();
    private final List<PageVisitResult> results = new ArrayList<>();

    private int batchCount;
    private int episodeCount;
    private int failureCount;
    private long loadNanos;
    private long detectNanos;
    private long writeNanos;

    public SplitPageVisitor(File outputRoot, boolean dryRun) {
        this.outputRoot = outputRoot;
        this.dryRun = dryRun;
    }

    @Override
    public void visit(WebtoonBatch batch) {
        batchCount++;
        System.out.println("배치 방문: " + batch.getName()
                + " (에피소드 " + batch.getEpisodes().size() + "개)");
    }

    @Override
    public void visit(WebtoonEpisode episode) {
        episodeCount++;
        System.out.println("\n에피소드 방문: " + episode.getName()
                + " (페이지 " + episode.getPages().size() + "개)");
    }

    @Override
    public void visit(WebtoonPage page) {
        File source = page.getImageFile();
        long totalStart = System.nanoTime();
        try {
            long loadStart = System.nanoTime();
            BufferedImage image = ImageIO.read(source);
            long currentLoad = System.nanoTime() - loadStart;
            if (image == null) {
                throw new IOException("지원하지 않거나 손상된 이미지");
            }

            long detectStart = System.nanoTime();
            List<Integer> cuts = detector.findCutPoints(image);
            long currentDetect = System.nanoTime() - detectStart;

            long currentWrite = 0;
            if (!dryRun) {
                long writeStart = System.nanoTime();
                writer.write(source, image, cuts, outputRoot);
                currentWrite = System.nanoTime() - writeStart;
            }

            PageVisitResult result = new PageVisitResult(
                    source,
                    new ArrayList<>(cuts),
                    currentLoad,
                    currentDetect,
                    currentWrite,
                    System.nanoTime() - totalStart);
            results.add(result);
            loadNanos += currentLoad;
            detectNanos += currentDetect;
            writeNanos += currentWrite;

            System.out.println("  페이지 방문/처리: " + source.getPath());
            System.out.println("    컷 위치: " + cuts);
            System.out.println("    결과 컷: " + result.pieceCount());
            printTime("    로딩", currentLoad);
            printTime("    탐색", currentDetect);
            if (!dryRun) {
                printTime("    저장", currentWrite);
            }
            printTime("    페이지 전체", result.totalNanos);
        } catch (IOException | RuntimeException exception) {
            failureCount++;
            System.err.println("  페이지 처리 실패: " + source + " ("
                    + exception.getMessage() + ")");
        }
    }

    public List<PageVisitResult> getResults() {
        return Collections.unmodifiableList(results);
    }

    public void printSummary(long programNanos) {
        int pieces = 0;
        for (PageVisitResult result : results) {
            pieces += result.pieceCount();
        }

        System.out.println("\n========== Composite + Visitor 실행 요약 ==========");
        System.out.println("방문 배치 / 에피소드 : " + batchCount + " / " + episodeCount);
        System.out.println("성공 / 실패 페이지   : "
                + results.size() + " / " + failureCount);
        System.out.println("전체 결과 컷         : " + pieces);
        printTime("누적 로딩", loadNanos);
        printTime("누적 탐색", detectNanos);
        printTime(dryRun ? "누적 저장(생략)" : "누적 저장", writeNanos);
        printTime("프로그램 전체", programNanos);
        if (!dryRun) {
            System.out.println("출력 폴더            : " + outputRoot.getPath());
        }
    }

    private static void printTime(String label, long nanos) {
        System.out.printf(
                Locale.ROOT, "%-20s: %,.3f ms%n", label, nanos / 1_000_000.0);
    }
}

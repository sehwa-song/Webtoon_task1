import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 객체 구조를 바꾸지 않고 컷 분석 연산을 추가하는 Concrete Visitor. */
final class CutAnalysisVisitor implements WebtoonVisitor {
    private final CutPointStrategy strategy;
    private final Map<Path, List<Integer>> results = new LinkedHashMap<>();

    CutAnalysisVisitor(CutPointStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public void enterBatch(WebtoonBatch batch) {
        results.clear();
    }

    @Override
    public void enterEpisode(WebtoonEpisode episode) {
        // 에피소드별 초기화가 필요하면 이 지점에 추가한다.
    }

    @Override
    public void visitPage(WebtoonPage page) {
        results.put(page.path(), strategy.findCutPoints(page.image()));
    }

    @Override
    public void leaveEpisode(WebtoonEpisode episode) {
        // 에피소드별 통계나 후처리가 필요하면 이 지점에 추가한다.
    }

    @Override
    public void leaveBatch(WebtoonBatch batch) {
        // 배치 전체 통계나 후처리가 필요하면 이 지점에 추가한다.
    }

    long checksum() {
        long checksum = 1;
        for (Map.Entry<Path, List<Integer>> entry : results.entrySet()) {
            checksum = 31 * checksum + entry.getKey().hashCode();
            checksum = 31 * checksum + entry.getValue().hashCode();
        }
        return checksum;
    }

    Map<Path, List<Integer>> results() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(results));
    }
}

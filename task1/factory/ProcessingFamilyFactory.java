package task1.factory;

/**
 * 서로 함께 사용하는 이미지 로더, 컷 탐색기, 저장기를 한 제품군으로 생성하는
 * Abstract Factory입니다.
 */
public interface ProcessingFamilyFactory {
    ImageRepository createImageRepository();
    CutPointDetector createCutPointDetector();
    PieceWriter createPieceWriter();
    String name();
}

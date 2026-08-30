package task1.factory;

/** 동일 이미지를 재처리할 때 디스크 접근을 줄이는 캐시 제품군입니다. */
public final class CachedProcessingFactory
        implements ProcessingFamilyFactory {
    @Override
    public ImageRepository createImageRepository() {
        return SingletonLruImageRepository.getInstance();
    }

    @Override
    public CutPointDetector createCutPointDetector() {
        return RgbMarginCutPointDetector.getInstance();
    }

    @Override
    public PieceWriter createPieceWriter() {
        return JpegPieceWriter.getInstance();
    }

    @Override
    public String name() {
        return "cached-family";
    }
}

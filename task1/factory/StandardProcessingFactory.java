package task1.factory;

/** 매번 디스크에서 읽는 표준 제품군을 생성하는 구체 팩토리입니다. */
public final class StandardProcessingFactory
        implements ProcessingFamilyFactory {
    @Override
    public ImageRepository createImageRepository() {
        return new DiskImageRepository();
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
        return "standard-family";
    }
}

package variants.factory;

/** Concrete factory whose repository decodes the file on every load request. */
public final class StandardProcessingFactory implements ProcessingFamilyFactory {
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
        return "ABSTRACT_FACTORY_STANDARD_DISK";
    }
}

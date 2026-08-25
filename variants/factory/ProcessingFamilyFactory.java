package variants.factory;

/**
 * Abstract Factory for a mutually compatible image-loading, cut-detection,
 * and piece-writing product family.
 */
public interface ProcessingFamilyFactory {
    ImageRepository createImageRepository();

    CutPointDetector createCutPointDetector();

    PieceWriter createPieceWriter();

    String name();
}



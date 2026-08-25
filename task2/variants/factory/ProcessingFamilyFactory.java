package task2.variants.factory;

/** Abstract Factory for compatible loading, detection, and writing products. */
public interface ProcessingFamilyFactory {
    ImageRepository createImageRepository();

    ContentRegionDetector createContentRegionDetector();

    RegionWriter createRegionWriter();

    String name();
}

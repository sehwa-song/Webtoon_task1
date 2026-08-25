package task2.variants.factory;

/** Concrete family that reloads every source image from disk. */
public final class StandardProcessingFactory implements ProcessingFamilyFactory {
    @Override
    public ImageRepository createImageRepository() {
        return new DiskImageRepository();
    }

    @Override
    public ContentRegionDetector createContentRegionDetector() {
        return new RgbMarginContentRegionDetector();
    }

    @Override
    public RegionWriter createRegionWriter() {
        return new JpegRegionWriter();
    }

    @Override
    public String name() {
        return "TASK2_ABSTRACT_FACTORY_STANDARD_DISK";
    }
}

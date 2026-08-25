package task2.variants.decorator;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;

/** Concrete Decorator that verifies bounds, ordering, and non-overlap. */
public final class ValidatingContentRegionDetector
        extends ContentRegionDetectorDecorator {
    public ValidatingContentRegionDetector(ContentRegionDetector delegate) {
        super(delegate);
    }

    @Override
    public List<ContentRegion> findContentRegions(BufferedImage image) {
        Objects.requireNonNull(image, "image");
        List<ContentRegion> regions = Objects.requireNonNull(
                delegate.findContentRegions(image),
                "delegate result");

        int previousBottom = -1;
        for (int index = 0; index < regions.size(); index++) {
            ContentRegion region = regions.get(index);
            if (region == null) {
                throw new IllegalStateException(
                        "Detector returned a null region at index " + index);
            }
            if (region.top() < 0
                    || region.bottomExclusive() > image.getHeight()
                    || region.top() >= region.bottomExclusive()) {
                throw new IllegalStateException(
                        "Detector returned an out-of-bounds region: " + region);
            }
            if (previousBottom > region.top()) {
                throw new IllegalStateException(
                        "Detector returned overlapping or unsorted regions: "
                                + regions);
            }
            previousBottom = region.bottomExclusive();
        }
        return regions;
    }

    @Override
    public String name() {
        return "validation(" + delegate.name() + ")";
    }
}

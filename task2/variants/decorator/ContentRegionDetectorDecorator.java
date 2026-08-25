package task2.variants.decorator;

import java.util.Objects;

/** Base Decorator that stores another detector Component. */
public abstract class ContentRegionDetectorDecorator
        implements ContentRegionDetector {
    protected final ContentRegionDetector delegate;

    protected ContentRegionDetectorDecorator(ContentRegionDetector delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }
}

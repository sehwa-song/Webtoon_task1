package task2.variants.template;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Creator of the fixed Task2 extraction workflow.
 *
 * <p>The public {@link #extract(Path, Path, boolean)} method is the Template
 * Method: subclasses may implement individual primitive operations, but they
 * cannot reorder loading, detection, optional writing, and result creation.</p>
 */
public abstract class AbstractWebtoonExtractor {

    /**
     * Final Template Method.
     * normalize/load -> detect -> optional write -> immutable result
     */
    public final ExtractionResult extract(
            Path source, Path outputRoot, boolean writeFiles) throws IOException {
        Path normalizedSource = normalizeSource(source);
        BufferedImage image = loadAndValidate(normalizedSource);
        List<ContentRegion> regions = detectAndFreeze(image);

        List<Path> writtenFiles = Collections.emptyList();
        if (writeFiles) {
            Path normalizedOutput = Objects.requireNonNull(
                    outputRoot, "outputRoot").toAbsolutePath().normalize();
            writtenFiles = freezePaths(doWriteContentRegions(
                    normalizedSource, image, regions, normalizedOutput));
        }

        return new ExtractionResult(
                normalizedSource, regions, writtenFiles, extractorName());
    }

    /** File-loading plus detection entry point without output writing. */
    public final List<ContentRegion> detect(Path source) throws IOException {
        Path normalizedSource = normalizeSource(source);
        return detectAndFreeze(loadAndValidate(normalizedSource));
    }

    /** Preloaded detector-only entry point for later JMH comparisons. */
    public final List<ContentRegion> detect(BufferedImage image) {
        return detectAndFreeze(validateImage(image));
    }

    public final String name() {
        return extractorName();
    }

    /** Primitive operation: concrete subclass chooses how to load a source. */
    protected abstract BufferedImage doLoadImage(Path normalizedSource)
            throws IOException;

    /** Primitive operation: concrete subclass owns the complete detection algorithm. */
    protected abstract List<ContentRegion> doDetectContentRegions(
            BufferedImage image);

    /** Primitive operation: concrete subclass chooses how kept regions are written. */
    protected abstract List<Path> doWriteContentRegions(
            Path source,
            BufferedImage image,
            List<ContentRegion> regions,
            Path outputRoot) throws IOException;

    /** Hook used for diagnostics and benchmark result labels. */
    protected abstract String extractorName();

    private static Path normalizeSource(Path source) {
        return Objects.requireNonNull(source, "source")
                .toAbsolutePath().normalize();
    }

    private BufferedImage loadAndValidate(Path normalizedSource)
            throws IOException {
        BufferedImage image = doLoadImage(normalizedSource);
        if (image == null) {
            throw new IOException(
                    "Unsupported or damaged image: " + normalizedSource);
        }
        return validateImage(image);
    }

    private static BufferedImage validateImage(BufferedImage image) {
        Objects.requireNonNull(image, "image");
        if (image.getWidth() <= 0 || image.getHeight() <= 0) {
            throw new IllegalArgumentException("Image dimensions must be positive");
        }
        return image;
    }

    private List<ContentRegion> detectAndFreeze(BufferedImage image) {
        List<ContentRegion> detected = Objects.requireNonNull(
                doDetectContentRegions(image), "detected regions");
        List<ContentRegion> copy = new ArrayList<ContentRegion>(detected.size());
        int previousBottom = -1;
        for (ContentRegion region : detected) {
            if (region == null
                    || region.top() < 0
                    || region.bottomExclusive() > image.getHeight()
                    || region.top() < previousBottom) {
                throw new IllegalStateException(
                        "Regions must be ordered, non-overlapping, and in bounds: "
                                + detected);
            }
            copy.add(region);
            previousBottom = region.bottomExclusive();
        }
        return Collections.unmodifiableList(copy);
    }

    private static List<Path> freezePaths(List<Path> paths) {
        Objects.requireNonNull(paths, "written paths");
        List<Path> copy = new ArrayList<Path>(paths.size());
        for (Path path : paths) {
            copy.add(Objects.requireNonNull(path, "written path")
                    .toAbsolutePath().normalize());
        }
        return Collections.unmodifiableList(copy);
    }
}

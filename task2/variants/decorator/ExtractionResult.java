package task2.variants.decorator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable result of processing one source image. */
public final class ExtractionResult {
    private final Path source;
    private final List<ContentRegion> regions;
    private final List<Path> writtenFiles;

    ExtractionResult(
            Path source,
            List<ContentRegion> regions,
            List<Path> writtenFiles) {
        this.source = source;
        this.regions = Collections.unmodifiableList(
                new ArrayList<ContentRegion>(regions));
        this.writtenFiles = Collections.unmodifiableList(
                new ArrayList<Path>(writtenFiles));
    }

    public Path source() {
        return source;
    }

    public List<ContentRegion> regions() {
        return regions;
    }

    public List<Path> writtenFiles() {
        return writtenFiles;
    }
}

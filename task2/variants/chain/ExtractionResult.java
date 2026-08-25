package task2.variants.chain;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable result of one service-level Task2 extraction operation. */
public final class ExtractionResult {
    private final Path source;
    private final List<ContentRegion> regions;
    private final List<Path> writtenFiles;
    private final String detectorName;

    ExtractionResult(
            Path source,
            List<ContentRegion> regions,
            List<Path> writtenFiles,
            String detectorName) {
        this.source = source;
        this.regions = Collections.unmodifiableList(
                new ArrayList<ContentRegion>(regions));
        this.writtenFiles = Collections.unmodifiableList(
                new ArrayList<Path>(writtenFiles));
        this.detectorName = detectorName;
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

    public String detectorName() {
        return detectorName;
    }

    public int regionCount() {
        return regions.size();
    }
}


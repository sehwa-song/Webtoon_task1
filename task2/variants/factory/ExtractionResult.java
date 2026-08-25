package task2.variants.factory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable result of one service-level Task2 extraction operation. */
public final class ExtractionResult {
    private final Path source;
    private final List<ContentRegion> regions;
    private final List<Path> writtenFiles;
    private final String factoryName;

    ExtractionResult(
            Path source,
            List<ContentRegion> regions,
            List<Path> writtenFiles,
            String factoryName) {
        this.source = source;
        this.regions = Collections.unmodifiableList(
                new ArrayList<ContentRegion>(regions));
        this.writtenFiles = Collections.unmodifiableList(
                new ArrayList<Path>(writtenFiles));
        this.factoryName = factoryName;
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

    public String factoryName() {
        return factoryName;
    }

    public int regionCount() {
        return regions.size();
    }
}

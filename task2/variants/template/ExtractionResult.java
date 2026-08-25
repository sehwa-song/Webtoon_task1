package task2.variants.template;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable result produced by the final Template Method workflow. */
public final class ExtractionResult {
    private final Path source;
    private final List<ContentRegion> regions;
    private final List<Path> writtenFiles;
    private final String extractorName;

    ExtractionResult(
            Path source,
            List<ContentRegion> regions,
            List<Path> writtenFiles,
            String extractorName) {
        this.source = Objects.requireNonNull(source, "source");
        this.regions = Collections.unmodifiableList(
                new ArrayList<ContentRegion>(regions));
        this.writtenFiles = Collections.unmodifiableList(
                new ArrayList<Path>(writtenFiles));
        this.extractorName = Objects.requireNonNull(extractorName, "extractorName");
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

    public String extractorName() {
        return extractorName;
    }

    public int regionCount() {
        return regions.size();
    }
}

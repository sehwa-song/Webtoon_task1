package task2.variants.proxy;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable result of one Proxy client extraction operation. */
public final class ExtractionResult {
    private final Path source;
    private final List<ContentRegion> regions;
    private final List<Path> writtenFiles;

    ExtractionResult(
            Path source,
            List<ContentRegion> regions,
            List<Path> writtenFiles) {
        this.source = Objects.requireNonNull(source, "source");
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

    public int regionCount() {
        return regions.size();
    }
}

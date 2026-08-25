package variants.factory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable result of one service-level page processing operation. */
public final class ProcessingResult {
    private final Path source;
    private final List<Integer> cutPoints;
    private final List<Path> writtenPieces;
    private final String factoryName;

    ProcessingResult(
            Path source,
            List<Integer> cutPoints,
            List<Path> writtenPieces,
            String factoryName) {
        this.source = source;
        this.cutPoints = Collections.unmodifiableList(
                new ArrayList<Integer>(cutPoints));
        this.writtenPieces = Collections.unmodifiableList(
                new ArrayList<Path>(writtenPieces));
        this.factoryName = factoryName;
    }

    public Path source() {
        return source;
    }

    public List<Integer> cutPoints() {
        return cutPoints;
    }

    public List<Path> writtenPieces() {
        return writtenPieces;
    }

    public String factoryName() {
        return factoryName;
    }

    public int pieceCount() {
        return cutPoints.size() - 1;
    }
}

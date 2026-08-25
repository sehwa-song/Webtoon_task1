package task2.variants.chain;

/** Moves removal boundaries inward so a bubble or drawing is not clipped. */
final class BoundaryForegroundHandler extends RemovalCandidateHandler {
    @Override
    protected RemovalCandidate handle(RemovalCandidate candidate) {
        ChainContentRegionDetector.RemovalZone adjusted =
                ChainContentRegionDetector.protectForegroundAtBoundaries(
                        candidate.image,
                        candidate.rows,
                        candidate.removal,
                        candidate.scale);
        return adjusted == null ? null : candidate.withRemoval(adjusted);
    }
}

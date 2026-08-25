package task2.variants.chain;

/** Selects the longest protected-content-free blank run in the separator. */
final class SafeRemovalHandler extends RemovalCandidateHandler {
    @Override
    protected RemovalCandidate handle(RemovalCandidate candidate) {
        ChainContentRegionDetector.RemovalZone removal =
                ChainContentRegionDetector.chooseSafeRemoval(
                        candidate.separator,
                        candidate.safeBlank,
                        candidate.scale);
        return removal == null ? null : candidate.withRemoval(removal);
    }
}

package task2.variants.chain;

/** Vetoes a short gap crossed by a connected foreground component. */
final class ForegroundBridgeVetoHandler extends RemovalCandidateHandler {
    @Override
    protected RemovalCandidate handle(RemovalCandidate candidate) {
        boolean bridge = ChainContentRegionDetector.hasForegroundBridge(
                candidate.image,
                candidate.rows,
                candidate.removal,
                candidate.scale);
        return bridge ? null : candidate;
    }
}

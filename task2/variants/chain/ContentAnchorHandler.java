package task2.variants.chain;

/** Rejects an edge zone or a zone without real content on both sides. */
final class ContentAnchorHandler extends RemovalCandidateHandler {
    @Override
    protected RemovalCandidate handle(RemovalCandidate candidate) {
        ChainContentRegionDetector.SeparatorZone zone = candidate.separator;
        if (zone.coreStart == 0
                || zone.coreEnd == candidate.image.getHeight() - 1) {
            return null;
        }
        if (!ChainContentRegionDetector.hasContentAnchor( //위쪽
                candidate.rows, zone.start - 1, -1, candidate.scale)
                || !ChainContentRegionDetector.hasContentAnchor( // 아래쪽
                candidate.rows, zone.end + 1, 1, candidate.scale)) {
            return null;
        }
        return candidate;
    }
}

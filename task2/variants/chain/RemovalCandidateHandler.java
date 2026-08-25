package task2.variants.chain;

/**
 * Link abstraction for the Chain of Responsibility.
 *
 * <p>Each handler either rejects a candidate by returning {@code null},
 * adjusts it, or delegates the accepted candidate to the next handler.</p>
 */
abstract class RemovalCandidateHandler {
    private RemovalCandidateHandler next;

    final RemovalCandidateHandler linkWith(RemovalCandidateHandler nextHandler) {
        if (nextHandler == null) {
            throw new IllegalArgumentException("nextHandler must not be null");
        }
        next = nextHandler;
        return nextHandler;
    }

    final RemovalCandidate process(RemovalCandidate candidate) {
        if (candidate == null) {
            throw new IllegalArgumentException("candidate must not be null");
        }
        RemovalCandidate handled = handle(candidate);
        if (handled == null || next == null) {
            return handled;
        }
        return next.process(handled);
    }

    protected abstract RemovalCandidate handle(RemovalCandidate candidate);
}

package task2.variants.strategy;

/**
 * A vertical content interval that must be kept in the Task2 output.
 * {@code top} is inclusive and {@code bottomExclusive} is exclusive.
 */
public final class ContentRegion {
    private final int top;
    private final int bottomExclusive;

    public ContentRegion(int top, int bottomExclusive) {
        if (top < 0 || bottomExclusive <= top) {
            throw new IllegalArgumentException(
                    "Invalid content region: [" + top + ", " + bottomExclusive + ")");
        }
        this.top = top;
        this.bottomExclusive = bottomExclusive;
    }

    public int top() {
        return top;
    }

    public int bottomExclusive() {
        return bottomExclusive;
    }

    public int height() {
        return bottomExclusive - top;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ContentRegion)) {
            return false;
        }
        ContentRegion that = (ContentRegion) other;
        return top == that.top && bottomExclusive == that.bottomExclusive;
    }

    @Override
    public int hashCode() {
        return 31 * top + bottomExclusive;
    }

    @Override
    public String toString() {
        return "[" + top + "," + bottomExclusive + ")";
    }
}

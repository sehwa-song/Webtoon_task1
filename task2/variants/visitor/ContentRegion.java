package task2.variants.visitor;

/** 보존해서 출력할 세로 콘텐츠 구간. top은 포함하고 bottom은 포함하지 않는다. */
public final class ContentRegion {
    private final int top;
    private final int bottomExclusive;

    public ContentRegion(int top, int bottomExclusive) {
        if (top < 0 || bottomExclusive <= top) {
            throw new IllegalArgumentException(
                    "잘못된 콘텐츠 구간: [" + top + ", " + bottomExclusive + ")");
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

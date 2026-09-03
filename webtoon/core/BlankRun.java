package webtoon.core;

/** 끝 좌표는 포함하지 않는 연속 여백 구간. */
public record BlankRun(int top, int bottom) {
    public BlankRun {
        if (top < 0 || bottom <= top) {
            throw new IllegalArgumentException("잘못된 여백 구간");
        }
    }

    public int middle() {
        return (top + bottom) / 2;
    }
}

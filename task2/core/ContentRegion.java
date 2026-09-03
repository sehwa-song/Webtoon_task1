package task2.core;

public record ContentRegion(int top, int bottom) {
    public ContentRegion {
        if (top < 0 || bottom <= top) throw new IllegalArgumentException("잘못된 콘텐츠 구간");
    }
}

package task1.chain;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 책임 연쇄를 통과하는 하나의 연속 여백 후보입니다. */
public final class BlankZoneContext {
    private final BufferedImage image;
    private final int startY;
    private final int endY;
    private final List<int[]> coloredRuns = new ArrayList<>();
    private boolean neutralHeightQualified;
    private boolean coloredStable;
    private int cutY = -1;

    public BlankZoneContext(
            BufferedImage image,
            int startY,
            int endY) {
        this.image = image;
        this.startY = startY;
        this.endY = endY;
    }

    public BufferedImage getImage() {
        return image;
    }

    public int getStartY() {
        return startY;
    }

    public int getEndY() {
        return endY;
    }

    public boolean isNeutralHeightQualified() {
        return neutralHeightQualified;
    }

    public void setNeutralHeightQualified(boolean qualified) {
        this.neutralHeightQualified = qualified;
    }

    public void addColoredRun(int start, int end) {
        coloredRuns.add(new int[] {start, end});
    }

    public List<int[]> getColoredRuns() {
        return Collections.unmodifiableList(coloredRuns);
    }

    public boolean isColoredStable() {
        return coloredStable;
    }

    public void setColoredStable(boolean coloredStable) {
        this.coloredStable = coloredStable;
    }

    public int getCutY() {
        return cutY;
    }

    public void setCutY(int cutY) {
        this.cutY = cutY;
    }
}

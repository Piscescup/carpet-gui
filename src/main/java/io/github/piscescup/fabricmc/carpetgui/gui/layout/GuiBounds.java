package io.github.piscescup.fabricmc.carpetgui.gui.layout;

/**
 * Explicit x/y/width/height geometry, rather than Minecraft's ambiguous four-int overload.
 */
public record GuiBounds(
    int left,
    int top,
    int width,
    int height
) {
    public GuiBounds {
        if (width < 0 || height < 0) throw new IllegalArgumentException("Negative GUI size");
    }

    public int right() {
        return left + width;
    }

    public int bottom() {
        return top + height;
    }

    public boolean contains(double x, double y) {
        return x >= left && x < right() && y >= top && y < bottom();
    }

    public boolean intersects(int y, int height) {
        return y + height > top && y < bottom();
    }

    public boolean encloses(GuiBounds other) {
        return other.left >= left && other.top >= top && other.right() <= right() && other.bottom() <= bottom();
    }
}

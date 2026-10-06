package io.github.piscescup.fabricmc.carpetgui.gui.layout;

/**
 * Menu geometry with a scrollable row limit. The usable area must fit at least one row.
 */
public record DropdownLayout(
    GuiBounds bounds,
    int rows
) {
    public static final int ROW_HEIGHT = 22;

    public static DropdownLayout below(GuiBounds anchor, GuiBounds area, int optionCount) {
        int top = Math.clamp(anchor.bottom() + 2, area.top(), Math.max(area.top(), area.bottom() - 26));
        int rows = Math.clamp(optionCount, 1, Math.clamp((area.bottom() - top - 4) / ROW_HEIGHT, 1, 8));
        int width = Math.min(anchor.width(), area.width());
        int left = Math.clamp(anchor.left(), area.left(), area.right() - width);
        return new DropdownLayout(new GuiBounds(left, top, width, rows * ROW_HEIGHT + 4), rows);
    }

    public int rowAt(double x, double y) {
        if (!bounds.contains(x, y) || y < bounds.top() + 2 || y >= bounds.bottom() - 2) return -1;
        return (int) ((y - bounds.top() - 2) / ROW_HEIGHT);
    }
}

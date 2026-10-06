package io.github.piscescup.fabricmc.carpetgui.gui.layout;

/**
 * Shared columns for every rule, independent of scroll and category.
 */
public record RuleRowLayout(
    int labelLeft,
    int labelWidth,
    GuiBounds value,
    GuiBounds reset,
    GuiBounds saveDefault
) {
    public static RuleRowLayout at(int screenWidth, int rowY) {
        return at(screenWidth, rowY, false);
    }

    public static RuleRowLayout at(int screenWidth, int rowY, boolean persistent) {
        int labelLeft = 38;
        int valueLeft = Math.max(130, screenWidth * 23 / 100);
        int resetWidth = Math.clamp(screenWidth / 10, 36, 58);
        int defaultWidth = persistent ? Math.clamp(screenWidth / 8, 74, 90) : 0;
        int valueWidth = Math.max(
            1,
            Math.min(
                260,
                Math.min(screenWidth * 19 / 100, screenWidth - valueLeft - resetWidth - defaultWidth - (persistent ? 40 : 32))
            )
        );

        return new RuleRowLayout(
            labelLeft, Math.max(1, valueLeft - labelLeft - 10),
            new GuiBounds(valueLeft, rowY + 2, valueWidth, 20),
            new GuiBounds(valueLeft + valueWidth + 8, rowY + 2, resetWidth, 20),
            new GuiBounds(valueLeft + valueWidth + resetWidth + 16, rowY + 2, defaultWidth, 20)
        );
    }
}

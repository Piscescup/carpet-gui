package io.github.piscescup.fabricmc.carpetgui.gui.render;

import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Shared pixel-edged popup appearance for both GUI implementations. */
public final class PopupStyle {
    public static final int BACKGROUND = 0xFF202020;
    public static final int BORDER = 0xFF707070;
    public static final int HOVER = 0xFF454545;
    public static final int SEPARATOR = 0xFF555555;
    public static final int SELECTED_TEXT = 0xFFFFFF55;

    private PopupStyle() {}

    public static void background(TGuiGraphics graphics, int x, int y, int width, int height) {
        background(graphics::fillColor, x, y, width, height);
    }

    public static void background(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        background((left, top, w, h, color) -> graphics.fill(left, top, left + w, top + h, color), x, y, width, height);
    }

    private static void background(Fill fill, int x, int y, int width, int height) {
        if (width < 5 || height < 5) {
            fill.draw(x, y, width, height, BACKGROUND);
            return;
        }
        // One-pixel stepped corners, with no rounded antialiasing or button bevels.
        fill.draw(x + 1, y + 1, width - 2, height - 2, BACKGROUND);
        fill.draw(x + 2, y, width - 4, 1, BORDER);
        fill.draw(x + 2, y + height - 1, width - 4, 1, BORDER);
        fill.draw(x, y + 2, 1, height - 4, BORDER);
        fill.draw(x + width - 1, y + 2, 1, height - 4, BORDER);
        fill.draw(x + 1, y + 1, 1, 1, BORDER);
        fill.draw(x + width - 2, y + 1, 1, 1, BORDER);
        fill.draw(x + 1, y + height - 2, 1, 1, BORDER);
        fill.draw(x + width - 2, y + height - 2, 1, 1, BORDER);
    }

    @FunctionalInterface
    private interface Fill {
        void draw(int x, int y, int width, int height, int color);
    }
}

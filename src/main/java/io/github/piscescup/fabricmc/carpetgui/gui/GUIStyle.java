/*
 * This file is part of the Carpet GUI project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  Fallen_Breath and contributors
 *
 * Carpet GUI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Carpet GUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Carpet GUI.  If not, see <https://www.gnu.org/licenses/>.
 */

//#if MC >= 12111
package io.github.piscescup.fabricmc.carpetgui.gui;

import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
//#if MC >= 260000
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#endif

/** Shared pixel-rounded popup appearance for both GUI implementations. */
public final class GUIStyle {
    public static final int BACKGROUND = 0xFF383838;
    public static final int BORDER = 0xFF707070;
    public static final int HOVER = 0xFF505050;
    public static final int SEPARATOR = 0xFF606060;
    public static final int SELECTED_TEXT = 0xFFFFFF55;
    public static final double TEXT_SCALE = 0.85;
    public static final double SMALL_TEXT_SCALE = 0.65;
    public static final int MENU_HEIGHT = 17;
    public static final int TAB_Y = 20;
    public static final int TAB_HEIGHT = 18;
    public static final int BODY_Y = TAB_Y + TAB_HEIGHT;
    public static final int CONTROL_HEIGHT = 20;
    public static final int RULE_HEIGHT = 25;
    public static final int TEXT_COLOR = 0xFFFFFFFF;
    public static final int MUTED_COLOR = 0xFF999999;
    public static final int ACCENT_COLOR = 0xFFFFFF55;
    public static final int FOCUS_COLOR = 0xFFAAFFFF;
    public static final int PANEL_COLOR = 0x18000000;
    public static final int BACKDROP_COLOR = 0x14000000;
    public static final int FRAME_COLOR = 0x10000000;
    public static final int CARD_COLOR = 0x20000000;
    public static final int RULE_BACKGROUND_COLOR = 0x28000000;
    public static final int BORDER_COLOR = 0xAA151515;
    public static final int NEW_RULE_COLOR = 0xFFFF5555;
    public static final int MODIFIED_RULE_COLOR = 0xFF55AAFF;
    public static final int STAGED_RULE_COLOR = 0xFF55FF55;
    public static final int REMOTE_RULE_COLOR = 0xFFFFAA00;

    private GUIStyle() {}

    public static void background(TGuiGraphics graphics, int x, int y, int width, int height) {
        background(graphics::fillColor, x, y, width, height);
    }

    //#if MC >= 260000
    public static void background(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        background((left, top, w, h, color) -> graphics.fill(left, top, left + w, top + h, color), x, y, width, height);
    }
    //#endif

    private static void background(Fill fill, int x, int y, int width, int height) {
        if (width < 5 || height < 5) {
            fill.draw(x, y, width, height, BACKGROUND);
            return;
        }
        // One-pixel stepped corners preserve Minecraft's pixel-art appearance.
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
//#endif

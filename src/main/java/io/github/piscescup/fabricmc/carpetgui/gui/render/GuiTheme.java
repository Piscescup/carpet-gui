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

//#if MC >= 260000
package io.github.piscescup.fabricmc.carpetgui.gui.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Classic gray form styling, shared by screens and widgets.
 */
public final class GuiTheme {
    public static final int TEXT = 0xFFFFFFFF;
    public static final int MUTED = 0xFF888888;
    public static final int LINE = 0xFF666666;

    private GuiTheme() {
    }

    public static String fit(Font font, String text, int width) {
        if (font.width(text) <= width) return text;
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width("..."))) + "...";
    }

    public static void smallText(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int width) {
        graphics.pose()
            .pushMatrix();
        try {
            graphics.pose()
                .translate(x, y)
                .scale(0.75f);
            graphics.text(font, fit(font, text, (int) (width / 0.75f)), 0, 0, MUTED, false);
        } finally {
            graphics.pose()
                .popMatrix();
        }
    }

    public static void button(
        GuiGraphicsExtractor graphics, int x, int y, int width, int height,
        boolean highlighted, boolean selected
    ) {
        int right = x + width;
        int bottom = y + height;
        int fill = selected ? 0xFF303030 : highlighted ? 0xFF808080 : 0xFF686868;
        graphics.fill(x, y, right, bottom, fill);
        graphics.outline(x, y, width, height, 0xFF080808);
        int light = selected ? 0xFF505050 : highlighted ? 0xFFFFFFFF : 0xFFC0C0C0;
        graphics.horizontalLine(x + 1, right - 2, y + 1, light);
        graphics.verticalLine(x + 1, y + 1, bottom - 2, light);
        graphics.horizontalLine(x + 2, right - 2, bottom - 2, 0xFF303030);
        graphics.verticalLine(right - 2, y + 2, bottom - 2, 0xFF303030);
    }
}
//#endif

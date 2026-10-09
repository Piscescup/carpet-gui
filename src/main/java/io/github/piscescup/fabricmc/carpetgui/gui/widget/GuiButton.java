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
package io.github.piscescup.fabricmc.carpetgui.gui.widget;

import io.github.piscescup.fabricmc.carpetgui.gui.render.GuiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Extendable themed button; native input, focus, narration and tooltip behavior remain intact.
 */
public class GuiButton
    extends Button
{
    private boolean selected;

    public GuiButton(int x, int y, int width, int height, Component label, OnPress action) {
        super(x, y, width, height, label, action, DEFAULT_NARRATION);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiTheme.button(graphics, getX(), getY(), getWidth(), getHeight(), active && isHoveredOrFocused(), selected);
        var font = Minecraft.getInstance().font;
        var lines = font.split(getMessage(), Math.max(1, getWidth() - 10));
        if (!lines.isEmpty()) {
            graphics.centeredText(
                font, lines.getFirst(), getX() + getWidth() / 2,
                getY() + (getHeight() - font.lineHeight) / 2, selected || !active ? GuiTheme.MUTED : GuiTheme.TEXT
            );
        }
    }
}
//#endif

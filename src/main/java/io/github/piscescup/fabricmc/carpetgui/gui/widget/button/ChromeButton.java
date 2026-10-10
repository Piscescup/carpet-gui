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
package io.github.piscescup.fabricmc.carpetgui.gui.widget.button;

import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
import net.minecraft.network.chat.Component;

/**
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public class ChromeButton extends Button {
    private boolean selected;

    public ChromeButton(Component text, Runnable action) {
        super(text, action);
    }

    @Override
    public void setSelected(boolean value) {
        super.setSelected(value);
        selected = value;
    }

    @Override
    public void renderCallback(TGuiGraphics graphics) {
        var bounds = getBounds();
        if (selected || isHoveredOrFocused()) {
            graphics.fillColor(bounds.x, bounds.y, bounds.width, bounds.height, selected ? 0xFF414345 : 0xA0454545);
        }
        if (selected) graphics.fillColor(bounds.x, bounds.endY - 1, bounds.width, 1, 0xFF4288B8);
    }
}
//#endif

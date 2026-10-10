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

import static io.github.piscescup.fabricmc.carpetgui.gui.workspace.WorkspaceStyle.FOCUS_COLOR;

/**
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public class IconButton extends Button {
    public IconButton(Runnable action) {
        super(Component.empty(), action);
    }

    @Override
    public void renderCallback(TGuiGraphics graphics) {
        super.renderCallback(graphics);
        var bounds = getBounds();
        graphics.drawOutlineIn(
            bounds.x, bounds.y, bounds.width, bounds.height, isHoveredOrFocused() ?
                FOCUS_COLOR : 0xFF707070
        );
    }
}
//#endif

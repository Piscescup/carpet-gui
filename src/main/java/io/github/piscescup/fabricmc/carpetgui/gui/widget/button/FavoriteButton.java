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
import com.thecsdev.commonmc.api.client.gui.widget.TButtonWidget;
import io.github.piscescup.fabricmc.carpetgui.gui.GUIStyle;
import io.github.piscescup.fabricmc.carpetgui.resources.WorkspaceActionTextures;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Outline and filled heart textures switch with the favorite state.
 */
public final class FavoriteButton extends TButtonWidget.Transparent {
    private final BooleanSupplier favorite;

    public FavoriteButton(BooleanSupplier favorite, Runnable action) {
        this.favorite = Objects.requireNonNull(favorite, "favorite");
        getLabel().setText(Component.empty());
        eClicked.addListener(ignored -> action.run());
    }

    @Override
    public void renderCallback(TGuiGraphics graphics) {
        var bounds = getBounds();
        if (isHoveredOrFocused()) {
            graphics.fillColor(bounds.x, bounds.y, bounds.width, bounds.height, 0x50393939);
            graphics.drawOutlineIn(bounds.x, bounds.y, bounds.width, bounds.height, GUIStyle.FOCUS_COLOR);
        }
        WorkspaceActionTextures.draw(
            graphics, bounds, favorite.getAsBoolean()
                ? WorkspaceActionTextures.Icon.FAVORITE_ON : WorkspaceActionTextures.Icon.FAVORITE_OFF,
            GUIStyle.TEXT_COLOR
        );
    }
}
//#endif

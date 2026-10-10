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
package io.github.piscescup.fabricmc.carpetgui.gui.widget.icons;

import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
import com.thecsdev.commonmc.api.client.gui.widget.TButtonWidget;
import io.github.piscescup.fabricmc.carpetgui.gui.GUIStyle;
import io.github.piscescup.fabricmc.carpetgui.resources.WorkspaceActionTextures;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;


public final class RuleActionIcon extends TButtonWidget.Transparent {
    private final boolean lock;
    private final BooleanSupplier saved;

    public RuleActionIcon(boolean lock, BooleanSupplier saved, Runnable action) {
        this.lock = lock;
        this.saved = saved;
        getLabel().setText(Component.empty());
        getLabel().hoverableProperty()
            .set(false, RuleActionIcon.class);
        eClicked.addListener(ignored -> {
            if (enabledProperty().getZ()) action.run();
        });
    }

    @Override
    public void renderCallback(TGuiGraphics graphics) {
        var bounds = getBounds();
        boolean closed = lock && saved.getAsBoolean();
        var icon = lock ? closed ?
            WorkspaceActionTextures.Icon.LOCK_CLOSED :
            WorkspaceActionTextures.Icon.LOCK_OPEN
            : WorkspaceActionTextures.Icon.RESET;
        WorkspaceActionTextures.draw(
            graphics, bounds, icon,
            lock || enabledProperty().getZ() ? GUIStyle.TEXT_COLOR : GUIStyle.MUTED_COLOR
        );
        if (enabledProperty().getZ() && isHoveredOrFocused()) {
            graphics.drawOutlineIn(bounds.x, bounds.y, bounds.width, bounds.height, GUIStyle.FOCUS_COLOR);
        }
    }
}
//#endif

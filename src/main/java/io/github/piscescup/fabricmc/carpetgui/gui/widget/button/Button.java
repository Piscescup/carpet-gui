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
import io.github.piscescup.fabricmc.carpetgui.gui.workspace.WorkspaceStyle;
import net.minecraft.network.chat.Component;

import static io.github.piscescup.fabricmc.carpetgui.gui.workspace.WorkspaceStyle.*;

/**
 *
 * @author REN YuanTong
 * @since
 */
public class Button extends TButtonWidget {
    private boolean selected;

    public Button(Component text, Runnable action) {
        getLabel().setText(text);
        getLabel().hoverableProperty()
            .set(false, Button.class);
        getLabel().textScaleProperty()
            .set(WorkspaceStyle.TEXT_SCALE, Button.class);
        //#if MC >= 260000
        getLabel().dropShadowProperty()
            .set(false, Button.class);
        //#endif
        eClicked.addListener(ignored -> action.run());
    }

    public void setSelected(boolean value) {
        selected = value;
        getLabel().textColorProperty()
            .set(selected ? WorkspaceStyle.ACCENT_COLOR : WorkspaceStyle.TEXT_COLOR, Button.class);
    }

    @Override
    public void renderCallback(TGuiGraphics graphics) {
        var bounds = getBounds();
        graphics.fillColor(
            bounds.x, bounds.y, bounds.width, bounds.height,
            selected ? 0xB0454545 : isHoveredOrFocused() ? 0x90393939 : 0x50202020
        );
        if (selected) graphics.fillColor(bounds.x, bounds.endY - 2, bounds.width, 2, 0xFF4288B8);
        if (isHoveredOrFocused()) {
            graphics.drawOutlineIn(
                bounds.x, bounds.y, bounds.width, bounds.height,
                FOCUS_COLOR
            );
        }
        getLabel().textColorProperty()
            .set(
                !isFocusable() ? WorkspaceStyle.MUTED_COLOR : selected ? WorkspaceStyle.ACCENT_COLOR : WorkspaceStyle.TEXT_COLOR,
                Button.class
            );
    }
}
//#endif

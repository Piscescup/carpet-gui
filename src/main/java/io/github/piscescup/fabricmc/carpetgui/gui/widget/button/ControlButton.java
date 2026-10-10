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

import com.thecsdev.commonmc.api.client.gui.widget.TButtonWidget;
import net.minecraft.network.chat.Component;

import static io.github.piscescup.fabricmc.carpetgui.gui.GUIStyle.TEXT_SCALE;

/**
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public final class ControlButton extends TButtonWidget {
    public ControlButton(Component text, Runnable action) {
        getLabel().setText(text);
        getLabel().hoverableProperty()
            .set(false, ControlButton.class);
        getLabel().textScaleProperty()
            .set(TEXT_SCALE, ControlButton.class);
        //#if MC >= 260000
        getLabel().dropShadowProperty()
            .set(false, ControlButton.class);
        //#endif
        getLabel().wrapTextProperty()
            .set(false, ControlButton.class);
        eClicked.addListener(ignored -> action.run());
    }
}
//#endif

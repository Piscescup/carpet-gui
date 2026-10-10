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
package io.github.piscescup.fabricmc.carpetgui.gui.workspace;

import com.thecsdev.common.util.enumerations.CompassDirection;
import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.label.TLabelElement;
import com.thecsdev.commonmc.api.client.gui.widget.TDropdownWidget;
import io.github.piscescup.fabricmc.carpetgui.gui.GUIStyle;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

/** Shared flat workspace visuals; no rule-system dependency. */
public final class WorkspaceStyle {

    private WorkspaceStyle() {}

    public static TLabelElement label(TElement parent, Component text, int x, int y, int width, int height, int color) {
        TLabelElement label = new TLabelElement(text);
        label.setBounds(x, y, Math.max(1, width), Math.max(1, height));
        label.textColorProperty().set(color, WorkspaceStyle.class);
        label.textScaleProperty().set(GUIStyle.TEXT_SCALE, WorkspaceStyle.class);
        //#if MC >= 260000
        label.dropShadowProperty().set(false, WorkspaceStyle.class);
        //#endif
        label.textAlignmentProperty().set(CompassDirection.NORTH_WEST, WorkspaceStyle.class);
        label.hoverableProperty().set(false, WorkspaceStyle.class);
        parent.add(label);
        return label;
    }

    public record Option<T>(T value, Component label) implements TDropdownWidget.Entry {
        @NonNull
        @Override
        public Component getDisplayName() {
            return label;
        }
    }

    /** Thin typed wrapper around TCDCommons' dropdown implementation. */
    public static final class Dropdown<T> extends TDropdownWidget<Option<T>> {
        public Dropdown(Option<T> selected) {
            super(selected);
            getLabel().hoverableProperty().set(false, Dropdown.class);
            getLabel().textScaleProperty().set(GUIStyle.TEXT_SCALE, Dropdown.class);
            //#if MC >= 260000
            getLabel().dropShadowProperty().set(false, Dropdown.class);
            //#endif
            getLabel().textAlignmentProperty().set(CompassDirection.WEST, Dropdown.class);
            getLabel().wrapTextProperty().set(false, Dropdown.class);
        }
    }
}
//#endif

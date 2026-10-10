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
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

/** Shared flat workspace visuals; no rule-system dependency. */
public final class WorkspaceStyle {
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

    private WorkspaceStyle() {}

    public static TLabelElement label(TElement parent, Component text, int x, int y, int width, int height, int color) {
        TLabelElement label = new TLabelElement(text);
        label.setBounds(x, y, Math.max(1, width), Math.max(1, height));
        label.textColorProperty().set(color, WorkspaceStyle.class);
        label.textScaleProperty().set(TEXT_SCALE, WorkspaceStyle.class);
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
            getLabel().textScaleProperty().set(TEXT_SCALE, Dropdown.class);
            //#if MC >= 260000
            getLabel().dropShadowProperty().set(false, Dropdown.class);
            //#endif
            getLabel().textAlignmentProperty().set(CompassDirection.WEST, Dropdown.class);
            getLabel().wrapTextProperty().set(false, Dropdown.class);
        }
    }
}
//#endif

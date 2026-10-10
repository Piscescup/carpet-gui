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
package io.github.piscescup.fabricmc.carpetgui.store;

import com.thecsdev.commonmc.api.client.gui.misc.TTextureElement;
import io.github.piscescup.fabricmc.carpetgui.References;
import net.minecraft.resources.Identifier;

/** Resource-backed original PNG icons; replace the files without changing layout code. */
public final class WorkspaceIcon extends TTextureElement {
    public enum Kind {
        SORT("filter_sort.png"),
        GROUP("filter_group.png"),
        DISTANCE("filter_unit_dist.png"),
        TIME("filter_unit_time.png");

        private final Identifier texture;

        Kind(String filename) {
            texture = References.fromPath("textures/gui/icons/" + filename);
        }

        public Identifier texture() { return texture; }
    }

    public WorkspaceIcon(Kind kind) {
        super(kind.texture());
        modeProperty().set(Mode.TEXTURE, WorkspaceIcon.class);
        hoverableProperty().set(false, WorkspaceIcon.class);
        focusableProperty().set(false, WorkspaceIcon.class);
    }

}
//#endif

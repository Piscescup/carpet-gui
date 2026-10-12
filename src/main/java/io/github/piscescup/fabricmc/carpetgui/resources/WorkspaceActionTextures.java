/*
 * This file is part of the Carpet GUI project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  PiscesCup and contributors
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

package io.github.piscescup.fabricmc.carpetgui.resources;

//#if MC >= 12111
import com.thecsdev.common.math.Bounds2i;
import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
//#else
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.util.UITexture;
//#endif
import io.github.piscescup.fabricmc.carpetgui.References;
import net.minecraft.resources.Identifier;

/** Resource textures are loaded and cached by Minecraft, never decoded per frame. */
public final class WorkspaceActionTextures {
    public enum Icon {
        LOCK_OPEN("lock_open"),
        LOCK_CLOSED("lock_closed"),
        RESET("reset"),
        FAVORITE_OFF("favorite_off"),
        FAVORITE_ON("favorite_on");

        private final Identifier texture;
        //#if MC <= 12110
        //$$ private final UITexture legacyTexture;
        //#endif
        Icon(String name) {
            texture = References.fromPath("textures/gui/icons/actions/" + name + ".png");
            //#if MC <= 12110
            //$$ legacyTexture = new UITexture(texture);
            //#endif
        }
    }

    private WorkspaceActionTextures() {}

    //#if MC >= 12111
    public static void draw(TGuiGraphics graphics, Bounds2i bounds, Icon icon, int tint) {
        int size = Math.max(1, Math.min(14, Math.min(bounds.width, bounds.height)));
            graphics.drawTexture(icon.texture, bounds.x + (bounds.width - size) / 2,
            bounds.y + (bounds.height - size) / 2, size, size, tint);
    }
    //#else
    //$$ public static UITexture texture(Icon icon) {
    //$$     return icon.legacyTexture;
    //$$ }
    //#endif
}

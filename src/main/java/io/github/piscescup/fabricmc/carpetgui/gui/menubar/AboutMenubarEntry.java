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
package io.github.piscescup.fabricmc.carpetgui.gui.menubar;

import com.thecsdev.commonmc.api.client.gui.ctxmenu.TContextMenu;
import io.github.piscescup.fabricmc.carpetgui.gui.workspace.CarpetWorkspaceEditor;
import io.github.piscescup.fabricmc.carpetgui.util.MsgUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** Information and help entries for Carpet GUI. */
public final class AboutMenubarEntry extends MenubarEntry {
    public static final AboutMenubarEntry INSTANCE = new AboutMenubarEntry();

    private AboutMenubarEntry() {}

    @Override
    public @NotNull Component getDisplayName() {
        return MsgUtils.tr("about");
    }

    @Override
    public @NotNull TContextMenu createContextMenu(
        @NotNull Minecraft client,
        @NotNull CarpetWorkspaceEditor editor
    ) {
        var builder = new TContextMenu.Builder(client);
        builder.addButton(Component.literal("Carpet GUI " + version()), ignored -> editor.selectPage(null));
        builder.addButton(MsgUtils.tr("help"), ignored -> editor.selectPage(null));
        return builder.build();
    }

    private static String version() {
        return FabricLoader.getInstance()
            .getModContainer("carpet-gui")
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
            .orElse("?");
    }
}
//#endif

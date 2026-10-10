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
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

/**
 * File operations for a Carpet workspace.
 * @author REN YuanTong
 * @since 1.0.0
 */
public final class FileMenubarEntry
    extends MenubarEntry
{
    public static final FileMenubarEntry INSTANCE = new FileMenubarEntry();

    private FileMenubarEntry() {}

    @NonNull
    @Override
    public Component getDisplayName() {
        return MsgUtils.tr("file");
    }

    @NonNull
    @Override
    public TContextMenu createContextMenu(
        @NonNull Minecraft client,
        @NonNull CarpetWorkspaceEditor editor
    ) {
        final var builder = new TContextMenu.Builder(client);
        builder.addButton(MsgUtils.tr("home"), ignored -> editor.selectPage(null));
        builder.addButton(MsgUtils.tr("refresh"), ignored -> editor.refreshWorkspace());
        return builder.build();
    }
}
//#endif

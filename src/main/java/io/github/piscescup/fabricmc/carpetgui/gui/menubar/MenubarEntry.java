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
import com.thecsdev.commonmc.api.client.gui.widget.TDropdownWidget;
import io.github.piscescup.fabricmc.carpetgui.gui.workspace.CarpetWorkspaceEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

/**
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public abstract class MenubarEntry implements TDropdownWidget.Entry {

    @NonNull
    @Override
    public abstract Component getDisplayName();

    @NonNull
    public abstract TContextMenu createContextMenu(
        @NotNull Minecraft client, @NotNull CarpetWorkspaceEditor editor
    );
}
//#endif

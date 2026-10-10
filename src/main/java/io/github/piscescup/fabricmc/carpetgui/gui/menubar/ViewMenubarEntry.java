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
import io.github.piscescup.fabricmc.carpetgui.gui.model.RulePage;
import io.github.piscescup.fabricmc.carpetgui.gui.pages.AllRulesPage;
import io.github.piscescup.fabricmc.carpetgui.gui.workspace.CarpetWorkspaceEditor;
import io.github.piscescup.fabricmc.carpetgui.store.ModIconStore;
import io.github.piscescup.fabricmc.carpetgui.util.Msg;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import static com.thecsdev.commonmc.resource.TComponent.gui;

/**
 * Navigation entries exposed by the current Carpet workspace.
 * @author REN YuanTong
 * @since
 */
public final class ViewMenubarEntry
    extends MenubarEntry
{
    public static final ViewMenubarEntry INSTANCE = new ViewMenubarEntry();

    private ViewMenubarEntry() {}

    @Override
    public @NonNull Component getDisplayName() {
        return Msg.tr("view");
    }

    @Override
    public @NonNull TContextMenu createContextMenu(@NotNull Minecraft client, @NotNull CarpetWorkspaceEditor editor) {
        var builder = new TContextMenu.Builder(client);
        builder.addButton(label("carpet-gui", Msg.tr("home"), editor.isSelected(null)),
            ignored -> editor.selectPage(null));
        builder.addSeparator();
        builder.addButton(label("carpet-gui", Msg.tr("rule_groups"),
                editor.isSelected(CarpetWorkspaceEditor.RULE_GROUPS_PAGE_ID)),
            ignored -> editor.selectPage(CarpetWorkspaceEditor.RULE_GROUPS_PAGE_ID));
        builder.addSeparator();
        for (RulePage page : editor.pages()) {
            String iconId = page instanceof AllRulesPage ? "carpet-gui" : page.id();
            builder.addButton(label(iconId, page.title(), editor.isSelected(page.id())),
                ignored -> editor.selectPage(page.id()));
        }
        return builder.build();
    }

    private Component label(String iconId, Component title, boolean selected) {
        var label = ModIconStore.INSTANCE.icon(iconId)
            .map(icon -> gui(icon).append(" ").append(title.copy()))
            .orElseGet(title::copy);
        return selected ? label.withStyle(ChatFormatting.YELLOW) : label;
    }
}
//#endif

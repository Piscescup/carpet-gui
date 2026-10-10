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

import com.thecsdev.common.scene.Node;
import com.thecsdev.commonmc.api.client.gui.ctxmenu.TContextMenu;
import com.thecsdev.commonmc.api.client.gui.misc.TTextureElement;
import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
import com.thecsdev.commonmc.api.client.gui.widget.TButtonWidget;
import com.thecsdev.common.util.enumerations.CompassDirection;
import io.github.piscescup.fabricmc.carpetgui.gui.pages.RulePage;
import io.github.piscescup.fabricmc.carpetgui.gui.pages.AllRulesPage;
import io.github.piscescup.fabricmc.carpetgui.gui.GUIStyle;
import io.github.piscescup.fabricmc.carpetgui.gui.workspace.CarpetWorkspaceEditor;
import io.github.piscescup.fabricmc.carpetgui.gui.workspace.WorkspaceStyle;
import io.github.piscescup.fabricmc.carpetgui.store.ModIconStore;
import io.github.piscescup.fabricmc.carpetgui.util.MsgUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;

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
        return MsgUtils.tr("view");
    }

    @Override
    public @NonNull TContextMenu createContextMenu(@NotNull Minecraft client, @NotNull CarpetWorkspaceEditor editor) {
        var entries = new ArrayList<MenuEntry>();
        entries.add(new MenuEntry("carpet-gui", MsgUtils.tr("home"), editor.isSelected(null),
            true, () -> editor.selectPage(null)));
        entries.add(new MenuEntry("carpet-gui", MsgUtils.tr("rule_groups"),
            editor.isSelected(CarpetWorkspaceEditor.RULE_GROUPS_PAGE_ID), true,
            () -> editor.selectPage(CarpetWorkspaceEditor.RULE_GROUPS_PAGE_ID)));
        for (RulePage page : editor.pages()) {
            String iconId = page instanceof AllRulesPage ? "carpet-gui" : page.id();
            entries.add(new MenuEntry(iconId, page.title(), editor.isSelected(page.id()), false,
                () -> editor.selectPage(page.id())));
        }

        int width = entries.stream()
            .mapToInt(entry -> client.font.width(entry.title()) + 32)
            .max()
            .orElse(100);
        width = Math.max(100, width);

        var builder = new TContextMenu.Builder(client);
        for (MenuEntry entry : entries) {
            builder.addElement(new MenuButton(entry, width));
            if (entry.separatorAfter()) builder.addSeparator();
        }
        return builder.build();
    }

    private record MenuEntry(
        String iconId,
        Component title,
        boolean selected,
        boolean separatorAfter,
        Runnable action
    ) {}

    private static final class MenuButton extends TButtonWidget.Transparent {
        private static final int HEIGHT = 22;
        private final MenuEntry entry;

        private MenuButton(MenuEntry entry, int width) {
            this.entry = entry;
            setBounds(0, 0, width, HEIGHT);
            getLabel().setText(entry.title());
            getLabel().textAlignmentProperty().set(CompassDirection.WEST, MenuButton.class);
            getLabel().textColorProperty().set(
                entry.selected() ? GUIStyle.SELECTED_TEXT : GUIStyle.TEXT_COLOR,
                MenuButton.class
            );
            eClicked.addListener(ignored -> {
                entry.action().run();
                findParent(element -> element instanceof TContextMenu)
                    .ifPresent(Node::remove);
            });
        }

        @Override
        protected void initCallback() {
            super.initCallback();
            var bounds = getBounds();
            getLabel().setBounds(bounds.x + 22, bounds.y, Math.max(1, bounds.width - 27), bounds.height);
            getLabel().wrapTextProperty().set(false, MenuButton.class);
            addIcon(bounds.x + 4, bounds.y + 4, 14);
        }

        private void addIcon(int x, int y, int size) {
            Identifier icon = "minecraft".equals(entry.iconId())
                ? Identifier.withDefaultNamespace("textures/block/grass_block_side.png")
                : ModIconStore.INSTANCE.icon(entry.iconId()).orElse(null);
            if (icon != null) {
                var texture = new TTextureElement(icon);
                texture.modeProperty().set(TTextureElement.Mode.TEXTURE, MenuButton.class);
                texture.hoverableProperty().set(false, MenuButton.class);
                texture.focusableProperty().set(false, MenuButton.class);
                texture.setBounds(x, y, size, size);
                add(texture);
                return;
            }

            String iconId = entry.iconId();
            String fallbackText = iconId == null || iconId.isBlank()
                ? "?"
                : iconId.substring(0, 1).toUpperCase();
            var fallback = WorkspaceStyle.label(
                this, Component.literal(fallbackText), x, y, size, size, GUIStyle.ACCENT_COLOR
            );
            fallback.textAlignmentProperty().set(CompassDirection.CENTER, MenuButton.class);
        }

        @Override
        public void renderCallback(@NotNull TGuiGraphics graphics) {
            if (!isHoveredOrFocused()) return;
            var bounds = getBounds();
            graphics.fillColor(bounds.x, bounds.y, bounds.width, bounds.height, GUIStyle.HOVER);
        }
    }
}
//#endif

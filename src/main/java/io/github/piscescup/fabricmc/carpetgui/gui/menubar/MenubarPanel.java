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

import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
import io.github.piscescup.fabricmc.carpetgui.gui.GUIStyle;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.button.ChromeButton;
import io.github.piscescup.fabricmc.carpetgui.gui.workspace.CarpetWorkspaceEditor;

import java.util.Objects;

/** Creates and owns the registered workspace menus. */
public final class MenubarPanel extends TElement {
    private final CarpetWorkspaceEditor editor;

    public MenubarPanel(CarpetWorkspaceEditor editor) {
        this.editor = Objects.requireNonNull(editor, "editor");
        focusableProperty().set(false, MenubarPanel.class);
        hoverableProperty().set(false, MenubarPanel.class);
    }

    @Override
    protected void initCallback() {
        var bounds = getBounds();
        int x = bounds.x + 1;
        for (MenubarEntry entry : MenubarEntryRegistry.entries()) {
            int buttonWidth = Math.max(36,
                (int) Math.ceil(getClient().font.width(entry.getDisplayName()) * GUIStyle.TEXT_SCALE) + 12);
            var button = new ChromeButton(entry.getDisplayName(), () -> {});
            button.setBounds(x, bounds.y + 1, buttonWidth, Math.max(1, bounds.height - 2));
            button.contextMenuProperty().set(
                ignored -> entry.createContextMenu(getClient(), editor), MenubarPanel.class
            );
            button.eClicked.addListener(ignored -> button.showContextMenu());
            add(button);
            x += buttonWidth;
        }
    }

    @Override
    public void renderCallback(TGuiGraphics graphics) {
        var bounds = getBounds();
        graphics.fillColor(bounds.x, bounds.y, bounds.width, bounds.height, 0x45000000);
        graphics.drawOutlineIn(bounds.x, bounds.y, bounds.width, bounds.height, GUIStyle.BORDER_COLOR);
    }
}
//#endif

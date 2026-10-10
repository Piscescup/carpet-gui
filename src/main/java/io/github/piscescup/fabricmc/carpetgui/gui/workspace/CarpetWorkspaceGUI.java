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

import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.panel.TPanelElement;
import io.github.piscescup.fabricmc.carpetgui.gui.CarpetWorkspaceScreen;
import io.github.piscescup.fabricmc.carpetgui.gui.menubar.MenubarPanel;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleEditResult;
import io.github.piscescup.fabricmc.carpetgui.gui.pages.RulePage;
import io.github.piscescup.fabricmc.carpetgui.api.RuleView;
import io.github.piscescup.fabricmc.carpetgui.gui.GUIStyle;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/** Top-level workspace composition: menubar plus the tabbed document surface. */
public final class CarpetWorkspaceGUI extends TElement {
    private final CarpetWorkspaceScreen screen;
    private final CarpetWorkspaceEditor editor;
    private WorkspaceDocumentInterface document;

    public CarpetWorkspaceGUI(CarpetWorkspaceScreen screen, CarpetWorkspaceEditor editor) {
        this.screen = Objects.requireNonNull(screen, "screen");
        this.editor = Objects.requireNonNull(editor, "editor");
    }

    @Override
    protected void initCallback() {
        var bounds = getBounds();
        int x = Math.max(4, bounds.width / 40);
        int width = Math.max(1, bounds.width - 2 * x);

        document = new WorkspaceDocumentInterface(screen, editor);
        document.setBounds(bounds);
        add(document);

        var menubar = new MenubarPanel(editor);
        menubar.setBounds(x, bounds.y, width, GUIStyle.MENU_HEIGHT);
        add(menubar);
    }

    public void cancelDrafts() {
        if (document != null) document.cancelDrafts();
    }

    public void closeResources() {
        if (document != null) document.closeResources();
    }

    public Component categoryLabel(RulePage page, String category) {
        return document.categoryLabel(page, category);
    }

    public void rebuildWorkspace() {
        if (document != null) document.rebuildWorkspace();
    }

    public void requestListRefresh() {
        if (document != null) document.requestListRefresh();
    }

    public void feedback(RuleView rule, RuleEditResult result) {
        if (document != null) document.feedback(rule, result);
    }

    public void showFeedback(Component message) {
        if (document != null) document.showFeedback(message);
    }

    public int textWidth(String text) {
        return document != null
            ? document.textWidth(text)
            : (int) Math.ceil(getClient().font.width(text) * GUIStyle.TEXT_SCALE);
    }

    public void addWorkspacePane(TPanelElement panel, int x, int y, int width, int height) {
        document.addWorkspacePane(panel, x, y, width, height);
    }

    public void addWorkspaceElement(TElement element) {
        document.addWorkspaceElement(element);
    }
}
//#endif

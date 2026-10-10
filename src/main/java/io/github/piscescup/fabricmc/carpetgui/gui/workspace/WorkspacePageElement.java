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
import io.github.piscescup.fabricmc.carpetgui.gui.pages.RulePage;

import java.util.Objects;

/** One document page hosted below the workspace tab strip. */
public abstract class WorkspacePageElement extends TElement {
    protected final WorkspaceDocumentInterface workspace;

    protected WorkspacePageElement(WorkspaceDocumentInterface workspace) {
        this.workspace = Objects.requireNonNull(workspace, "workspace");
        focusableProperty().set(false, WorkspacePageElement.class);
        hoverableProperty().set(false, WorkspacePageElement.class);
    }

    public static WorkspacePageElement create(WorkspaceDocumentInterface workspace, CarpetWorkspaceEditor editor) {
        Objects.requireNonNull(editor, "editor");
        if (editor.isSelected(CarpetWorkspaceEditor.RULE_GROUPS_PAGE_ID)) return new RuleGroups(workspace);
        RulePage page = editor.selectedPage();
        if (page == null) return new Home(workspace);
        return new Rules(workspace, page);
    }

    private static final class Home extends WorkspacePageElement {
        private Home(WorkspaceDocumentInterface workspace) {
            super(workspace);
        }

        @Override
        protected void initCallback() {
            var bounds = getBounds();
            workspace.buildHomePage(bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    private static final class Rules extends WorkspacePageElement {
        private final RulePage page;

        private Rules(WorkspaceDocumentInterface workspace, RulePage page) {
            super(workspace);
            this.page = Objects.requireNonNull(page, "page");
        }

        @Override
        protected void initCallback() {
            var bounds = getBounds();
            workspace.buildRulesPage(page, bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    private static final class RuleGroups extends WorkspacePageElement {
        private RuleGroups(WorkspaceDocumentInterface workspace) {
            super(workspace);
        }

        @Override
        protected void initCallback() {
            var bounds = getBounds();
            workspace.buildRuleGroupsPage(bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }
}
//#endif

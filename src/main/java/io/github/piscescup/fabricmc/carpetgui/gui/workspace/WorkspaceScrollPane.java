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

//#if MC >= 12111
package io.github.piscescup.fabricmc.carpetgui.gui.workspace;

import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.panel.TPanelElement;
import com.thecsdev.commonmc.api.client.gui.widget.TScrollBarWidget;

/** Reusable viewport with a vertical scrollbar and optional horizontal scrolling. */
final class WorkspaceScrollPane extends TElement {
    private final TPanelElement panel;
    WorkspaceScrollPane(TPanelElement panel) { this.panel = panel; }

    @Override protected void initCallback() {
        var bounds = getBounds();
        boolean horizontal = panel instanceof WorkspacePanel workspacePanel && workspacePanel.horizontalScrolling();
        int barSize = 8;
        panel.setBounds(bounds.x, bounds.y, Math.max(1, bounds.width - barSize),
            Math.max(1, bounds.height - (horizontal ? barSize : 0)));
        add(panel);
        var vertical = new TScrollBarWidget.Flat(panel, TScrollBarWidget.ScrollDirection.VERTICAL);
        vertical.setBounds(bounds.endX - 7, bounds.y, 7, Math.max(1, bounds.height - (horizontal ? barSize : 0)));
        add(vertical);
        if (horizontal) {
            var horizontalBar = new TScrollBarWidget.Flat(panel, TScrollBarWidget.ScrollDirection.HORIZONTAL);
            horizontalBar.setBounds(bounds.x, bounds.endY - 7, Math.max(1, bounds.width - barSize), 7);
            add(horizontalBar);
        }
    }
}
//#endif

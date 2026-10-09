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

import com.thecsdev.common.math.Point2d;
import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.panel.TPanelElement;
import com.thecsdev.commonmc.api.client.gui.util.TInputContext;

/** Scroll by wheel, keyboard or scrollbar, not by dragging blank panel space. */
class WorkspacePanel extends TPanelElement.Paintable {
    private final int padding;
    private TElement scrollExtent;

    WorkspacePanel(int padding) {
        super(WorkspaceStyle.PANEL, WorkspaceStyle.BORDER, WorkspaceStyle.BORDER);
        this.padding = padding;
        // Layouts already include their own insets. TPanelElement otherwise snaps the
        // children's bounding box to scrollPadding on the first wheel event, moving
        // both axes even when the content fits inside the viewport.
        scrollPaddingProperty().set(0, WorkspacePanel.class);
        eInitialized.addListener(ignored -> updateScrollExtent(padding));
        // These workspaces only expose a vertical scrollbar. Trackpads may still report a
        // horizontal wheel component, so clamp it here instead of letting content drift sideways.
        scrollAmountProperty().addFilter(point -> new Point2d(0, point.y), WorkspacePanel.class);
    }

    void refreshScrollExtent() {
        if (scrollExtent == null) return;
        updateScrollExtent(padding);
    }

    void scrollToTop() {
        int offset = Math.max(0, getBounds().y - getContentBounds().y);
        if (offset > 0) scroll(0, offset);
    }

    private void updateScrollExtent(int padding) {
        if (scrollExtent != null) remove(scrollExtent);
        var bounds = getBounds();
        var content = getContentBounds();
        scrollExtent = new TElement();
        scrollExtent.setBounds(bounds.x, bounds.y, bounds.width,
            Math.max(bounds.height, content.endY - bounds.y + padding));
        scrollExtent.focusableProperty().set(false, WorkspacePanel.class);
        scrollExtent.hoverableProperty().set(false, WorkspacePanel.class);
        add(scrollExtent);
    }

    @Override public boolean inputCallback(TInputContext.InputDiscoveryPhase phase, TInputContext context) {
        if (phase == TInputContext.InputDiscoveryPhase.MAIN && context.getInputType() == TInputContext.InputType.MOUSE_DRAG) return false;
        return super.inputCallback(phase, context);
    }
}

/** Paint-only container. Unlike TPanelElement, wheel/drag input must never move its children. */
final class WorkspaceStaticPanel extends TPanelElement.Paintable {
    WorkspaceStaticPanel(int background, int outline, int focusedOutline) {
        super(background, outline, focusedOutline);
        focusableProperty().set(false, WorkspaceStaticPanel.class);
        hoverableProperty().set(false, WorkspaceStaticPanel.class);
    }

    @Override public boolean inputCallback(TInputContext.InputDiscoveryPhase phase, TInputContext context) {
        return false;
    }
}
//#endif

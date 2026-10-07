package io.github.piscescup.fabricmc.carpetgui.gui.workspace;

import com.thecsdev.common.math.Point2d;
import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.panel.TPanelElement;
import com.thecsdev.commonmc.api.client.gui.util.TInputContext;

/** Scroll by wheel, keyboard or scrollbar, not by dragging blank panel space. */
class WorkspacePanel extends TPanelElement.Paintable {
    WorkspacePanel(int padding) {
        super(WorkspaceStyle.PANEL, WorkspaceStyle.BORDER, WorkspaceStyle.BORDER);
        scrollPaddingProperty().set(padding, WorkspacePanel.class);
        // These workspaces only expose a vertical scrollbar. Trackpads may still report a
        // horizontal wheel component, so clamp it here instead of letting content drift sideways.
        scrollAmountProperty().addFilter(point -> new Point2d(0, point.y), WorkspacePanel.class);
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

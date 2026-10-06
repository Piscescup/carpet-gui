package io.github.piscescup.fabricmc.carpetgui.gui.workspace;

import com.thecsdev.commonmc.api.client.gui.panel.TPanelElement;
import com.thecsdev.commonmc.api.client.gui.util.TInputContext;

/** Scroll by wheel, keyboard or scrollbar, not by dragging blank panel space. */
class WorkspacePanel extends TPanelElement.Paintable {
    WorkspacePanel(int padding) {
        super(WorkspaceStyle.PANEL, WorkspaceStyle.BORDER, WorkspaceStyle.BORDER);
        scrollPaddingProperty().set(padding, WorkspacePanel.class);
    }

    @Override public boolean inputCallback(TInputContext.InputDiscoveryPhase phase, TInputContext context) {
        if (phase == TInputContext.InputDiscoveryPhase.MAIN && context.getInputType() == TInputContext.InputType.MOUSE_DRAG) return false;
        return super.inputCallback(phase, context);
    }
}

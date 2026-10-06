package io.github.piscescup.fabricmc.carpetgui.gui.workspace;

import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.panel.TPanelElement;
import com.thecsdev.commonmc.api.client.gui.widget.TScrollBarWidget;

/** Reusable TCDCommons viewport with a vertical scrollbar only. */
final class WorkspaceScrollPane extends TElement {
    private final TPanelElement panel;
    WorkspaceScrollPane(TPanelElement panel) { this.panel = panel; }

    @Override protected void initCallback() {
        var bounds = getBounds();
        panel.setBounds(bounds.x, bounds.y, Math.max(1, bounds.width - 8), Math.max(1, bounds.height));
        add(panel);
        var vertical = new TScrollBarWidget.Flat(panel, TScrollBarWidget.ScrollDirection.VERTICAL);
        vertical.setBounds(bounds.endX - 7, bounds.y, 7, Math.max(1, bounds.height));
        add(vertical);
    }
}

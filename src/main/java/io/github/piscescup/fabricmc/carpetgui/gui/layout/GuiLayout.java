package io.github.piscescup.fabricmc.carpetgui.gui.layout;

import net.minecraft.client.gui.components.AbstractWidget;

public final class GuiLayout {
    private GuiLayout() {}

    public static void place(AbstractWidget widget, GuiBounds bounds) {
        widget.setPosition(bounds.left(), bounds.top());
        widget.setSize(bounds.width(), bounds.height());
    }
}

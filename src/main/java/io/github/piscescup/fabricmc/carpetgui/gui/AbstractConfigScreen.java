package io.github.piscescup.fabricmc.carpetgui.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Shared non-pausing, transparent-world configuration screen navigation.
 */
public abstract class AbstractConfigScreen
    extends Screen
{
    private final Screen parent;

    protected AbstractConfigScreen(Screen parent, Component title) {
        super(title);
        this.parent = parent;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xA0000000);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

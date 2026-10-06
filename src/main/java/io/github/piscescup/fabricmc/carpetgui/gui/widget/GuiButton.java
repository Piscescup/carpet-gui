package io.github.piscescup.fabricmc.carpetgui.gui.widget;

import io.github.piscescup.fabricmc.carpetgui.gui.render.GuiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Extendable themed button; native input, focus, narration and tooltip behavior remain intact.
 */
public class GuiButton
    extends Button
{
    private boolean selected;

    public GuiButton(int x, int y, int width, int height, Component label, OnPress action) {
        super(x, y, width, height, label, action, DEFAULT_NARRATION);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiTheme.button(graphics, getX(), getY(), getWidth(), getHeight(), active && isHoveredOrFocused(), selected);
        var font = Minecraft.getInstance().font;
        var lines = font.split(getMessage(), Math.max(1, getWidth() - 10));
        if (!lines.isEmpty()) {
            graphics.centeredText(
                font, lines.getFirst(), getX() + getWidth() / 2,
                getY() + (getHeight() - font.lineHeight) / 2, selected || !active ? GuiTheme.MUTED : GuiTheme.TEXT
            );
        }
    }
}

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

//#if MC >= 260000
package io.github.piscescup.fabricmc.carpetgui.gui.widget;

import io.github.piscescup.fabricmc.carpetgui.gui.layout.GuiBounds;
import io.github.piscescup.fabricmc.carpetgui.gui.model.Expandable;
import io.github.piscescup.fabricmc.carpetgui.gui.render.GuiTheme;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.function.Supplier;

/** Plain [+]/[-] control: no filled button background, just a label and small section ID. */
public final class SectionToggleButton extends GuiButton {
    private final Expandable section;
    private final Component label;
    private final String subtitle;
    private final Supplier<GuiBounds> viewport;
    private Boolean displayedExpanded;

    public SectionToggleButton(Expandable section, Component label, String subtitle, Supplier<GuiBounds> viewport) {
        super(0, 0, 1, 32, label, button -> {
            section.toggle();
            ((SectionToggleButton) button).refreshLabel();
        });
        this.section = section;
        this.label = label.copy();
        this.subtitle = subtitle;
        this.viewport = viewport;
        refreshLabel();
    }

    public void refreshLabel() {
        boolean expanded = section.isExpanded();
        if (displayedExpanded != null && displayedExpanded == expanded) return;
        displayedExpanded = expanded;
        setMessage(Component.literal(expanded ? "[-] " : "[+] ")
                .withStyle(expanded ? ChatFormatting.GRAY : ChatFormatting.RED)
                .append(label.copy().withStyle(ChatFormatting.WHITE)));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return viewport.get().contains(event.x(), event.y()) && super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        refreshLabel();
        var font = Minecraft.getInstance().font;
        graphics.horizontalLine(getX(), getRight(), getY() + 1, GuiTheme.LINE);
        if (isFocused()) graphics.outline(getX(), getY() + 3, getWidth(), getHeight() - 3, GuiTheme.MUTED);
        var lines = font.split(getMessage(), Math.max(1, getWidth() - 4));
        if (!lines.isEmpty()) graphics.text(font, lines.getFirst(), getX(), getY() + 7, GuiTheme.TEXT);
        GuiTheme.smallText(graphics, font, subtitle, getX() + 24, getY() + 20, getWidth() - 24);
    }
}
//#endif

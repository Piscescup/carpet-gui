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

package io.github.piscescup.fabricmc.carpetgui.gui.widget.button;

//#if MC >= 12111
import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
//#else
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.util.TDrawContext;
//$$ import io.github.thecsdev.tcdcommons.api.util.enumerations.HorizontalAlignment;
//#endif
import net.minecraft.network.chat.Component;

/**
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public class ChromeButton extends Button {
    private boolean selected;

    //#if MC >= 12111
    public ChromeButton(Component text, Runnable action) {
        super(text, action);
    }
    //#else
    //$$ public ChromeButton(int x, int y, int width, int height, Component text, Runnable action, boolean selected) {
    //$$     super(x, y, width, height, text, action, selected);
    //$$     this.selected = selected;
    //$$ }
    //$$
    //$$ @Override
    //$$ public ChromeButton align(HorizontalAlignment value) {
    //$$     super.align(value);
    //$$     return this;
    //$$ }
    //#endif

    @Override
    public void setSelected(boolean value) {
        super.setSelected(value);
        selected = value;
    }

    //#if MC >= 12111
    @Override
    public void renderCallback(TGuiGraphics graphics) {
        var bounds = getBounds();
        if (selected || isHoveredOrFocused()) {
            graphics.fillColor(bounds.x, bounds.y, bounds.width, bounds.height, selected ? 0xFF414345 : 0xA0454545);
        }
        if (selected) graphics.fillColor(bounds.x, bounds.endY - 1, bounds.width, 1, 0xFF4288B8);
    }
    //#else
    //$$ @Override
    //$$ public void render(TDrawContext graphics) {
    //$$     if (selected || isFocusedOrHovered()) {
    //$$         graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
    //$$             selected ? 0xFF414345 : 0xA0454545);
    //$$     }
    //$$     if (selected) graphics.fill(getX(), getY() + getHeight() - 1,
    //$$         getX() + getWidth(), getY() + getHeight(), 0xFF4288B8);
    //$$     if (getIcon() != null) renderIcon(graphics);
    //$$     else graphics.drawTElementTextTHC(getText(), alignment,
    //$$         getEnabled() ? selected ? 0xFFFFFF55 : 0xFFFFFFFF : 0xFF999999);
    //$$ }
    //#endif
}

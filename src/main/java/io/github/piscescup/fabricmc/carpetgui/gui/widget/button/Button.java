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
import com.thecsdev.commonmc.api.client.gui.widget.TButtonWidget;
//#else
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.util.TDrawContext;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.util.UITexture;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.widget.TButtonWidget;
//$$ import io.github.thecsdev.tcdcommons.api.util.enumerations.HorizontalAlignment;
//#endif
import io.github.piscescup.fabricmc.carpetgui.gui.GUIStyle;
import net.minecraft.network.chat.Component;

/**
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public class Button extends TButtonWidget {
    private boolean selected;

    //#if MC >= 12111
    public Button(Component text, Runnable action) {
        getLabel().setText(text);
        getLabel().hoverableProperty()
            .set(false, Button.class);
        getLabel().textScaleProperty()
            .set(GUIStyle.TEXT_SCALE, Button.class);
        //#if MC >= 260000
        getLabel().dropShadowProperty()
            .set(false, Button.class);
        //#endif
        eClicked.addListener(ignored -> action.run());
    }
    //#else
    //$$ protected HorizontalAlignment alignment = HorizontalAlignment.CENTER;
    //$$
    //$$ public Button(int x, int y, int width, int height, Component text, Runnable action, boolean selected) {
    //$$     super(x, y, width, height, text, ignored -> action.run());
    //$$     this.selected = selected;
    //$$ }
    //$$
    //$$ public Button withTexture(UITexture icon) {
    //$$     setIcon(icon);
    //$$     return this;
    //$$ }
    //$$
    //$$ public Button align(HorizontalAlignment value) {
    //$$     alignment = value;
    //$$     return this;
    //$$ }
    //#endif

    public void setSelected(boolean value) {
        selected = value;
        //#if MC >= 12111
        getLabel().textColorProperty()
            .set(selected ? GUIStyle.ACCENT_COLOR : GUIStyle.TEXT_COLOR, Button.class);
        //#endif
    }

    //#if MC >= 12111
    @Override
    public void renderCallback(TGuiGraphics graphics) {
        var bounds = getBounds();
        graphics.fillColor(
            bounds.x, bounds.y, bounds.width, bounds.height,
            selected ? 0xB0454545 : isHoveredOrFocused() ? 0x90393939 : 0x50202020
        );
        if (selected) graphics.fillColor(bounds.x, bounds.endY - 2, bounds.width, 2, 0xFF4288B8);
        if (isHoveredOrFocused()) {
            graphics.drawOutlineIn(
                bounds.x, bounds.y, bounds.width, bounds.height,
                GUIStyle.FOCUS_COLOR
            );
        }
        getLabel().textColorProperty()
            .set(
                !isFocusable() ? GUIStyle.MUTED_COLOR : selected ? GUIStyle.ACCENT_COLOR : GUIStyle.TEXT_COLOR,
                Button.class
            );
    }
    //#else
    //$$ @Override
    //$$ public void render(TDrawContext graphics) {
    //$$     int fill = selected ? 0x70505018 : isFocusedOrHovered() ? 0x60393939 : 0x30000000;
    //$$     graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), fill);
    //$$     int border = isFocusedOrHovered() ? GUIStyle.FOCUS_COLOR : GUIStyle.BORDER_COLOR;
    //$$     graphics.fill(getX(), getY(), getX() + getWidth(), getY() + 1, border);
    //$$     graphics.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), border);
    //$$     graphics.fill(getX(), getY(), getX() + 1, getY() + getHeight(), border);
    //$$     graphics.fill(getX() + getWidth() - 1, getY(), getX() + getWidth(), getY() + getHeight(), border);
    //$$     if (getIcon() != null) renderIcon(graphics);
    //$$     else graphics.drawTElementTextTHC(getText(), alignment,
    //$$         getEnabled() ? selected ? GUIStyle.ACCENT_COLOR : GUIStyle.TEXT_COLOR : GUIStyle.MUTED_COLOR);
    //$$ }
    //#endif
}

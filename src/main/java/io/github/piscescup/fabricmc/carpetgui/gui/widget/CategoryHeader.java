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

package io.github.piscescup.fabricmc.carpetgui.gui.widget;

//#if MC >= 12111
import com.thecsdev.common.util.enumerations.CompassDirection;
import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
import com.thecsdev.commonmc.api.client.gui.widget.TButtonWidget;
import io.github.piscescup.fabricmc.carpetgui.gui.workspace.WorkspaceStyle;
//#else
//$$ import io.github.piscescup.fabricmc.carpetgui.gui.widget.button.Button;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.other.TLabelElement;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.util.TDrawContext;
//$$ import io.github.thecsdev.tcdcommons.api.util.enumerations.HorizontalAlignment;
//#endif
import io.github.piscescup.fabricmc.carpetgui.gui.GUIStyle;
import net.minecraft.network.chat.Component;

/**
 * Plain yellow section name; the unobtrusive right-hand indicator retains fold discoverability.
 */
public final class CategoryHeader extends
    //#if MC >= 12111
    TButtonWidget.Transparent {
    //#else
    //$$ Button {
    //#endif
    private final boolean expanded;

    //#if MC >= 12111
    public CategoryHeader(Component category, boolean expanded, Runnable action) {
        this.expanded = expanded;
        getLabel().setText(category);
        getLabel().hoverableProperty()
            .set(false, CategoryHeader.class);
        getLabel().textColorProperty()
            .set(GUIStyle.ACCENT_COLOR, CategoryHeader.class);
        getLabel().textScaleProperty()
            .set(GUIStyle.TEXT_SCALE, CategoryHeader.class);
        //#if MC >= 260000
        getLabel().dropShadowProperty()
            .set(false, CategoryHeader.class);
        //#endif
        getLabel().textAlignmentProperty()
            .set(CompassDirection.WEST, CategoryHeader.class);
        getLabel().wrapTextProperty()
            .set(false, CategoryHeader.class);
        eClicked.addListener(ignored -> action.run());
    }
    //#else
    //$$ public CategoryHeader(int x, int y, int width, int height,
    //$$                       Component category, boolean expanded, Runnable action) {
    //$$     super(x, y, width, height, Component.empty(), action, false);
    //$$     this.expanded = expanded;
    //$$     TLabelElement title = new TLabelElement(
    //$$         x + 4, y, Math.max(1, width - 34), height, category);
    //$$     title.setTextColor(GUIStyle.ACCENT_COLOR);
    //$$     title.setTextHorizontalAlignment(HorizontalAlignment.LEFT);
    //$$     addChild(title, false);
    //$$     TLabelElement indicator = new TLabelElement(
    //$$         x + width - 28, y, 24, height, Component.literal(expanded ? "[-]" : "[+]"));
    //$$     indicator.setTextColor(GUIStyle.MUTED_COLOR);
    //$$     indicator.setTextHorizontalAlignment(HorizontalAlignment.RIGHT);
    //$$     addChild(indicator, false);
    //$$ }
    //#endif

    //#if MC >= 12111
    @Override
    protected void initCallback() {
        super.initCallback();
        var bounds = getBounds();
        getLabel().setBounds(bounds.x, bounds.y, Math.max(1, bounds.width - 25), bounds.height);
        var indicator = WorkspaceStyle.label(
            this, Component.literal(expanded ? "[-]" : "[+]"),
            bounds.endX - 22, bounds.y, 22, bounds.height, GUIStyle.MUTED_COLOR
        );
        indicator.textAlignmentProperty()
            .set(CompassDirection.EAST, CategoryHeader.class);
        indicator.textScaleProperty()
            .set(GUIStyle.SMALL_TEXT_SCALE, CategoryHeader.class);
    }

    @Override
    public void renderCallback(TGuiGraphics graphics) {
    }

    @Override
    public void postRenderCallback(TGuiGraphics graphics) {
        if (!isHoveredOrFocused()) return;
        var bounds = getBounds();
        graphics.fillColor(bounds.x, bounds.endY - 1, bounds.width, 1, 0x55FFFFFF);
    }
    //#else
    //$$ @Override
    //$$ public void render(TDrawContext graphics) {
    //$$     if (isFocusedOrHovered()) {
    //$$         graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x30607994);
    //$$     }
    //$$     graphics.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(),
    //$$         isFocusedOrHovered() ? 0x559EACBC : 0x40707070);
    //$$ }
    //#endif
}

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

package io.github.piscescup.fabricmc.carpetgui.gui.widget.icons;

//#if MC >= 12111
import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
import com.thecsdev.commonmc.api.client.gui.widget.TButtonWidget;
//#else
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.util.TDrawContext;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.widget.TButtonWidget;
//#endif
import io.github.piscescup.fabricmc.carpetgui.gui.GUIStyle;
import io.github.piscescup.fabricmc.carpetgui.resources.WorkspaceActionTextures;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;


public final class RuleActionIcon extends
    //#if MC >= 12111
    TButtonWidget.Transparent {
    //#else
    //$$ TButtonWidget {
    //#endif
    private final boolean lock;
    private final BooleanSupplier saved;

    //#if MC >= 12111
    public RuleActionIcon(boolean lock, BooleanSupplier saved, Runnable action) {
        this.lock = lock;
        this.saved = saved;
        getLabel().setText(Component.empty());
        getLabel().hoverableProperty()
            .set(false, RuleActionIcon.class);
        eClicked.addListener(ignored -> {
            if (enabledProperty().getZ()) action.run();
        });
    }
    //#else
    //$$ public RuleActionIcon(int x, int y, int width, int height,
    //$$                       boolean lock, BooleanSupplier saved, Runnable action) {
    //$$     super(x, y, width, height, Component.empty(), ignored -> action.run());
    //$$     this.lock = lock;
    //$$     this.saved = saved;
    //$$ }
    //#endif

    //#if MC >= 12111
    @Override
    public void renderCallback(TGuiGraphics graphics) {
        var bounds = getBounds();
        boolean closed = lock && saved.getAsBoolean();
        var icon = lock ? closed ?
            WorkspaceActionTextures.Icon.LOCK_CLOSED :
            WorkspaceActionTextures.Icon.LOCK_OPEN
            : WorkspaceActionTextures.Icon.RESET;
        WorkspaceActionTextures.draw(
            graphics, bounds, icon,
            lock || enabledProperty().getZ() ? GUIStyle.TEXT_COLOR : GUIStyle.MUTED_COLOR
        );
        if (enabledProperty().getZ() && isHoveredOrFocused()) {
            graphics.drawOutlineIn(bounds.x, bounds.y, bounds.width, bounds.height, GUIStyle.FOCUS_COLOR);
        }
    }
    //#else
    //$$ @Override
    //$$ public void render(TDrawContext graphics) {
    //$$     boolean closed = lock && saved.getAsBoolean();
    //$$     WorkspaceActionTextures.Icon icon = lock
    //$$         ? closed ? WorkspaceActionTextures.Icon.LOCK_CLOSED : WorkspaceActionTextures.Icon.LOCK_OPEN
    //$$         : WorkspaceActionTextures.Icon.RESET;
    //$$     setIcon(WorkspaceActionTextures.texture(icon));
    //$$     renderIcon(graphics);
    //$$     if (!getEnabled() || !isFocusedOrHovered()) return;
    //$$     int color = GUIStyle.FOCUS_COLOR;
    //$$     graphics.fill(getX(), getY(), getX() + getWidth(), getY() + 1, color);
    //$$     graphics.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), color);
    //$$     graphics.fill(getX(), getY(), getX() + 1, getY() + getHeight(), color);
    //$$     graphics.fill(getX() + getWidth() - 1, getY(), getX() + getWidth(), getY() + getHeight(), color);
    //$$ }
    //#endif
}

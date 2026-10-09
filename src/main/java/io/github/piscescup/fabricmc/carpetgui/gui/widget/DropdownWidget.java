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

import com.mojang.blaze3d.platform.InputConstants;
import io.github.piscescup.fabricmc.carpetgui.gui.layout.DropdownLayout;
import io.github.piscescup.fabricmc.carpetgui.gui.layout.GuiBounds;
import io.github.piscescup.fabricmc.carpetgui.gui.model.DropdownOption;
import io.github.piscescup.fabricmc.carpetgui.gui.model.DropdownState;
import io.github.piscescup.fabricmc.carpetgui.gui.render.GuiTheme;
import io.github.piscescup.fabricmc.carpetgui.gui.render.PopupStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Generic dropdown. Route open-menu events first and draw extractOverlay after the screen content. */
public final class DropdownWidget<T extends DropdownOption> extends GuiButton {
    private final DropdownState<T> state;
    private final Component caption;
    private final Supplier<GuiBounds> usableArea;

    public DropdownWidget(GuiBounds bounds, Component caption, List<? extends T> options, String selectedId,
                          Supplier<GuiBounds> usableArea, Consumer<T> onSelection) {
        super(bounds.left(), bounds.top(), bounds.width(), bounds.height(), caption,
                button -> ((DropdownWidget<?>) button).togglePopup());
        this.caption = caption.copy();
        this.usableArea = usableArea;
        state = new DropdownState<>(options, selectedId, option -> {
            setMessage(option.label());
            onSelection.accept(option);
        });
        setMessage(state.selected().label());
        setTooltip(Tooltip.create(caption));
    }

    public boolean isOpen() { return state.isOpen(); }
    public void closePopup() { state.close(); }
    public void synchronizeSelection(String id) {
        state.synchronizeSelection(id);
        setMessage(state.selected().label());
    }
    public void togglePopup() {
        if (!active || !visible) return;
        if (state.isOpen()) state.close();
        else {
            state.open();
            state.setVisibleRows(popupLayout().rows());
        }
    }
    public DropdownLayout popupLayout() {
        return DropdownLayout.below(new GuiBounds(getX(), getY(), getWidth(), getHeight()), usableArea.get(),
                state.options().size());
    }

    @NonNull
    @Override
    protected MutableComponent createNarrationMessage() {
        Component option = state.isOpen() ? state.options().get(state.highlightedIndex()).label() : state.selected().label();
        return caption.copy().append(": ").append(option);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiTheme.button(graphics, getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused(), false);
        var font = Minecraft.getInstance().font;
        graphics.text(font, GuiTheme.fit(font, getMessage().getString(), getWidth() - 26), getX() + 7,
                getY() + (getHeight() - font.lineHeight) / 2, GuiTheme.TEXT);
        for (int row = 0; row < 3; row++) {
            int y = getY() + 8 + (state.isOpen() ? 2 - row : row);
            graphics.fill(getRight() - 13 + row, y, getRight() - 6 - row, y + 1, GuiTheme.TEXT);
        }
    }

    public void extractOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!visible || !state.isOpen()) return;
        var layout = popupLayout();
        var bounds = layout.bounds();
        graphics.nextStratum();
        PopupStyle.background(graphics, bounds.left(), bounds.top(), bounds.width(), bounds.height());
        graphics.enableScissor(bounds.left() + 1, bounds.top() + 1, bounds.right() - 1, bounds.bottom() - 1);
        try {
            var font = Minecraft.getInstance().font;
            for (int row = 0; row < state.visibleRows(); row++) {
                int index = state.firstVisible() + row;
                int y = bounds.top() + 2 + row * DropdownLayout.ROW_HEIGHT;
                boolean hovered = layout.rowAt(mouseX, mouseY) == row;
                if (hovered || index == state.highlightedIndex())
                    graphics.fill(bounds.left() + 2, y, bounds.right() - 2, y + DropdownLayout.ROW_HEIGHT, PopupStyle.HOVER);
                String label = state.options().get(index).label().getString();
                graphics.text(font, GuiTheme.fit(font, label, bounds.width() - 16), bounds.left() + 8, y + 7,
                    index == state.selectedIndex() ? PopupStyle.SELECTED_TEXT : GuiTheme.TEXT, false);
                if (hovered && font.width(label) > bounds.width() - 22)
                    graphics.setTooltipForNextFrame(state.options().get(index).label(), mouseX, mouseY);
            }
        } finally { graphics.disableScissor(); }
        if (state.options().size() > state.visibleRows()) {
            int track = bounds.height() - 4;
            int thumb = Math.max(6, track * state.visibleRows() / state.options().size());
            int y = bounds.top() + 2 + (track - thumb) * state.firstVisible()
                    / (state.options().size() - state.visibleRows());
            graphics.fill(bounds.right() - 3, y, bounds.right() - 1, y + thumb, GuiTheme.MUTED);
        }
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (!state.isOpen()) return super.mouseClicked(event, doubleClick);
        var anchor = new GuiBounds(getX(), getY(), getWidth(), getHeight());
        if (anchor.contains(event.x(), event.y())) {
            if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) state.close();
            return true;
        }
        var layout = popupLayout();
        if (layout.bounds().contains(event.x(), event.y())) {
            int row = layout.rowAt(event.x(), event.y());
            if (row >= 0 && event.button() == InputConstants.MOUSE_BUTTON_LEFT) state.select(state.firstVisible() + row);
        } else state.close();
        return true; // Consume even outside clicks; do not activate widgets beneath the popup.
    }

    @Override
    public void mouseMoved(double x, double y) {
        if (!state.isOpen()) return;
        int row = popupLayout().rowAt(x, y);
        if (row >= 0) state.highlight(state.firstVisible() + row);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (!state.isOpen()) return false;
        if (vertical != 0) state.scrollBy(vertical > 0 ? -1 : 1);
        return true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (!active || !visible) { state.close(); return false; }
        if (!state.isOpen()) {
            if (isFocused() && (event.key() == InputConstants.KEY_DOWN || event.key() == InputConstants.KEY_UP)) {
                togglePopup();
                return true;
            }
            return super.keyPressed(event);
        }
        switch (event.key()) {
            case InputConstants.KEY_ESCAPE -> state.close();
            case InputConstants.KEY_UP -> state.move(-1);
            case InputConstants.KEY_DOWN -> state.move(1);
            case InputConstants.KEY_HOME -> state.highlight(0);
            case InputConstants.KEY_END -> state.highlight(state.options().size() - 1);
            case InputConstants.KEY_PAGEUP -> state.move(-state.visibleRows());
            case InputConstants.KEY_PAGEDOWN -> state.move(state.visibleRows());
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER, InputConstants.KEY_SPACE -> state.commit();
            case InputConstants.KEY_TAB -> { state.close(); return false; }
            default -> { }
        }
        return true;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused && state != null) state.close();
    }
}
//#endif

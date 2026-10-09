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

//#if MC >= 12111
package io.github.piscescup.fabricmc.carpetgui.gui.workspace;

import com.thecsdev.common.util.enumerations.CompassDirection;
import com.thecsdev.common.properties.ObjectProperty;
import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.label.TLabelElement;
import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
import com.thecsdev.commonmc.api.client.gui.widget.TButtonWidget;
import io.github.piscescup.fabricmc.carpetgui.gui.render.PopupStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Shared flat workspace visuals; no rule-system dependency. */
public final class WorkspaceStyle {
    public static final double TEXT_SCALE = 0.85;
    public static final double SMALL_TEXT_SCALE = 0.65;
    public static final int MENU_HEIGHT = 17;
    public static final int TAB_Y = 20;
    public static final int TAB_HEIGHT = 18;
    public static final int BODY_Y = TAB_Y + TAB_HEIGHT;
    public static final int CONTROL_HEIGHT = 20;
    public static final int RULE_HEIGHT = 25;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int MUTED = 0xFF999999;
    public static final int ACCENT = 0xFFFFFF55;
    public static final int FOCUS = 0xFFAAFFFF;
    public static final int PANEL = 0x18000000;
    public static final int BACKDROP = 0x14000000;
    public static final int FRAME = 0x10000000;
    public static final int CARD = 0x20000000;
    public static final int RULE_BACKGROUND = 0x28000000;
    public static final int BORDER = 0xAA151515;
    private WorkspaceStyle() {}

    public static TLabelElement label(TElement parent, Component text, int x, int y, int width, int height, int color) {
        TLabelElement label = new TLabelElement(text);
        label.setBounds(x, y, Math.max(1, width), Math.max(1, height));
        label.textColorProperty().set(color, WorkspaceStyle.class);
        label.textScaleProperty().set(TEXT_SCALE, WorkspaceStyle.class);
        //#if MC >= 260000
        label.dropShadowProperty().set(false, WorkspaceStyle.class);
        //#endif
        label.textAlignmentProperty().set(CompassDirection.NORTH_WEST, WorkspaceStyle.class);
        label.hoverableProperty().set(false, WorkspaceStyle.class);
        parent.add(label);
        return label;
    }

    public static class Button extends TButtonWidget {
        private boolean selected;
        public Button(Component text, Runnable action) {
            getLabel().setText(text);
            getLabel().hoverableProperty().set(false, Button.class);
            getLabel().textScaleProperty().set(TEXT_SCALE, Button.class);
            //#if MC >= 260000
            getLabel().dropShadowProperty().set(false, Button.class);
            //#endif
            eClicked.addListener(ignored -> action.run());
        }
        public void setSelected(boolean value) {
            selected = value;
            getLabel().textColorProperty().set(selected ? ACCENT : TEXT, Button.class);
        }
        @Override public void renderCallback(TGuiGraphics graphics) {
            var bounds = getBounds();
            graphics.fillColor(bounds.x, bounds.y, bounds.width, bounds.height,
                selected ? 0xB0454545 : isHoveredOrFocused() ? 0x90393939 : 0x50202020);
            if (selected) graphics.fillColor(bounds.x, bounds.endY - 2, bounds.width, 2, 0xFF4288B8);
            if (isHoveredOrFocused()) graphics.drawOutlineIn(bounds.x, bounds.y, bounds.width, bounds.height, FOCUS);
            getLabel().textColorProperty().set(!isFocusable() ? MUTED : selected ? ACCENT : TEXT, Button.class);
        }
    }

    /** Gray beveled controls share the same surface as closed dropdowns. */
    public static final class ControlButton extends Button {
        public ControlButton(Component text, Runnable action) {
            super(text, action);
            getLabel().wrapTextProperty().set(false, ControlButton.class);
        }
        @Override public void renderCallback(TGuiGraphics graphics) {
            controlSurface(graphics, this);
            getLabel().textColorProperty().set(enabledProperty().getZ() ? TEXT : MUTED, ControlButton.class);
        }
    }

    private static void controlSurface(TGuiGraphics graphics, TElement control) {
        var bounds = control.getBounds();
        graphics.fillColor(bounds.x, bounds.y, bounds.width, bounds.height, control.isHoveredOrFocused() ? 0xFF737373 : 0xFF626262);
        graphics.drawOutlineIn(bounds.x, bounds.y, bounds.width, bounds.height, control.isHoveredOrFocused() ? FOCUS : 0xFF151515);
        graphics.fillColor(bounds.x + 1, bounds.y + 1, Math.max(0, bounds.width - 2), 1, 0xFFAAAAAA);
        graphics.fillColor(bounds.x + 1, bounds.endY - 2, Math.max(0, bounds.width - 2), 1, 0xFF424242);
    }

    /** Menus/tabs share one continuous strip, with no individual button border. */
    public static class ChromeButton extends Button {
        private boolean selected;
        public ChromeButton(Component text, Runnable action) { super(text, action); }
        @Override public void setSelected(boolean value) { super.setSelected(value); selected = value; }
        @Override public void renderCallback(TGuiGraphics graphics) {
            var bounds = getBounds();
            if (selected || isHoveredOrFocused()) {
                graphics.fillColor(bounds.x, bounds.y, bounds.width, bounds.height, selected ? 0xFF414345 : 0xA0454545);
            }
            if (selected) graphics.fillColor(bounds.x, bounds.endY - 1, bounds.width, 1, 0xFF4288B8);
        }
    }

    public static class IconButton extends Button {
        public IconButton(Runnable action) { super(Component.empty(), action); }
        @Override public void renderCallback(TGuiGraphics graphics) {
            super.renderCallback(graphics);
            var bounds = getBounds();
            graphics.drawOutlineIn(bounds.x, bounds.y, bounds.width, bounds.height, isHoveredOrFocused() ? FOCUS : 0xFF707070);
        }
    }

    /** Plain yellow section name; the unobtrusive right-hand indicator retains fold discoverability. */
    public static final class CategoryHeader extends TButtonWidget.Transparent {
        private final boolean expanded;
        public CategoryHeader(Component category, boolean expanded, Runnable action) {
            this.expanded = expanded;
            getLabel().setText(category);
            getLabel().hoverableProperty().set(false, CategoryHeader.class);
            getLabel().textColorProperty().set(ACCENT, CategoryHeader.class);
            getLabel().textScaleProperty().set(TEXT_SCALE, CategoryHeader.class);
            //#if MC >= 260000
            getLabel().dropShadowProperty().set(false, CategoryHeader.class);
            //#endif
            getLabel().textAlignmentProperty().set(CompassDirection.WEST, CategoryHeader.class);
            getLabel().wrapTextProperty().set(false, CategoryHeader.class);
            eClicked.addListener(ignored -> action.run());
        }
        @Override protected void initCallback() {
            super.initCallback();
            var bounds = getBounds();
            getLabel().setBounds(bounds.x, bounds.y, Math.max(1, bounds.width - 25), bounds.height);
            var indicator = label(this, Component.literal(expanded ? "[-]" : "[+]"),
                bounds.endX - 22, bounds.y, 22, bounds.height, MUTED);
            indicator.textAlignmentProperty().set(CompassDirection.EAST, CategoryHeader.class);
            indicator.textScaleProperty().set(SMALL_TEXT_SCALE, CategoryHeader.class);
        }
        @Override public void renderCallback(TGuiGraphics graphics) {}
        @Override public void postRenderCallback(TGuiGraphics graphics) {
            if (!isHoveredOrFocused()) return;
            var bounds = getBounds();
            graphics.fillColor(bounds.x, bounds.endY - 1, bounds.width, 1, 0x55FFFFFF);
        }
    }

    public record Option<T>(T value, Component label, boolean separatorAfter) {
        public Option(T value, Component label) {
            this(value, label, false);
        }
    }

    /** Image-backed actions keep the same hit boxes, tooltips and enabled behavior. */
    public static final class RuleActionIcon extends TButtonWidget.Transparent {
        private final boolean lock;
        private final BooleanSupplier saved;

        public RuleActionIcon(boolean lock, BooleanSupplier saved, Runnable action) {
            this.lock = lock;
            this.saved = saved;
            getLabel().setText(Component.empty());
            getLabel().hoverableProperty().set(false, RuleActionIcon.class);
            eClicked.addListener(ignored -> { if (enabledProperty().getZ()) action.run(); });
        }

        @Override public void renderCallback(TGuiGraphics graphics) {
            var bounds = getBounds();
            boolean closed = lock && saved.getAsBoolean();
            var icon = lock ? closed ? WorkspaceActionTextures.Icon.LOCK_CLOSED : WorkspaceActionTextures.Icon.LOCK_OPEN
                : WorkspaceActionTextures.Icon.RESET;
            WorkspaceActionTextures.draw(graphics, bounds, icon, lock || enabledProperty().getZ() ? TEXT : MUTED);
            if (enabledProperty().getZ() && isHoveredOrFocused()) {
                graphics.drawOutlineIn(bounds.x, bounds.y, bounds.width, bounds.height, FOCUS);
            }
        }
    }

    /** Outline and filled heart textures switch with the favorite state. */
    public static final class FavoriteButton extends TButtonWidget.Transparent {
        private final BooleanSupplier favorite;

        public FavoriteButton(BooleanSupplier favorite, Runnable action) {
            this.favorite = Objects.requireNonNull(favorite, "favorite");
            getLabel().setText(Component.empty());
            eClicked.addListener(ignored -> action.run());
        }

        @Override public void renderCallback(TGuiGraphics graphics) {
            var bounds = getBounds();
            if (isHoveredOrFocused()) {
                graphics.fillColor(bounds.x, bounds.y, bounds.width, bounds.height, 0x50393939);
                graphics.drawOutlineIn(bounds.x, bounds.y, bounds.width, bounds.height, FOCUS);
            }
            WorkspaceActionTextures.draw(graphics, bounds, favorite.getAsBoolean()
                ? WorkspaceActionTextures.Icon.FAVORITE_ON : WorkspaceActionTextures.Icon.FAVORITE_OFF, TEXT);
        }
    }

    /** All workspace dropdowns use the same popup as the top navigation menus. */
    public static final class Dropdown<T> extends TButtonWidget {
        private final ObjectProperty<Option<T>> selectedEntry = new ObjectProperty<>();
        private final Collection<Option<T>> entries = new LinkedHashSet<>();

        public Dropdown(Option<T> selected) {
            selectedEntry.set(selected, Dropdown.class);
            getLabel().setText(selected == null ? Component.empty() : selected.label());
            selectedEntry.addChangeListener((property, previous, current) ->
                getLabel().setText(current == null ? Component.empty() : current.label()));
            getLabel().hoverableProperty().set(false, Dropdown.class);
            getLabel().textScaleProperty().set(TEXT_SCALE, Dropdown.class);
            //#if MC >= 260000
            getLabel().dropShadowProperty().set(false, Dropdown.class);
            //#endif
            getLabel().textAlignmentProperty().set(CompassDirection.WEST, Dropdown.class);
            getLabel().wrapTextProperty().set(false, Dropdown.class);
            eClicked.addListener(ignored -> openMenu());
        }

        public ObjectProperty<Option<T>> selectedEntryProperty() { return selectedEntry; }
        public Collection<Option<T>> getEntries() { return entries; }

        @Override protected void initCallback() {
            super.initCallback();
            var bounds = getBounds();
            getLabel().setBounds(bounds.x + 6, bounds.y, Math.max(1, bounds.width - 23), bounds.height);
        }

        private void openMenu() {
            var screen = screenProperty().get();
            if (screen == null || entries.isEmpty()) return;
            var items = entries.stream().map(option -> new WorkspaceNavigationMenu.Entry(null, option.label(),
                selectedEntry.get() != null && Objects.equals(option.value(), selectedEntry.get().value()), option.separatorAfter(), () -> {
                    screen.focusedElementProperty().set(this, Dropdown.class);
                    selectedEntry.set(option, Dropdown.class);
                })).toList();
            var menu = new WorkspaceNavigationMenu(items, null);
            var anchor = getBounds();
            var area = screen.getBounds();
            int widest = entries.stream().mapToInt(option -> (int) Math.ceil(Minecraft.getInstance().font.width(option.label()) * TEXT_SCALE)).max().orElse(0);
            int width = Math.min(area.width, Math.max(anchor.width, widest + 28));
            int desiredHeight = menu.contentHeight() + WorkspaceNavigationMenu.PADDING * 2;
            int below = Math.max(0, area.endY - anchor.endY - 2);
            int above = Math.max(0, anchor.y - area.y - 2);
            boolean opensBelow = desiredHeight <= below || below >= above;
            int height = Math.min(desiredHeight, Math.max(1, opensBelow ? below : above));
            int x = Math.clamp(anchor.x, area.x, Math.max(area.x, area.endX - width));
            int y = Math.clamp(opensBelow ? anchor.endY + 2 : anchor.y - height - 2, area.y, Math.max(area.y, area.endY - height));
            menu.setBounds(x, y, width, height);
            screen.add(menu);
            menu.clearAndInit();
        }
        @Override public void renderCallback(TGuiGraphics graphics) {
            var bounds = getBounds();
            controlSurface(graphics, this);
            getLabel().textColorProperty().set(enabledProperty().getZ() ? TEXT : MUTED, Dropdown.class);
            int arrowX = bounds.endX - 13, arrowY = bounds.y + bounds.height / 2 - 1;
            for (int row = 0; row < 3; row++) {
                graphics.fillColor(arrowX + row, arrowY + row, 7 - row * 2, 1, enabledProperty().getZ() ? TEXT : MUTED);
            }
        }
    }
}
//#endif

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
import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.misc.TTextureElement;
import com.thecsdev.commonmc.api.client.gui.panel.TPanelElement;
import com.thecsdev.commonmc.api.client.gui.tooltip.TTooltip;
import com.thecsdev.commonmc.api.client.gui.util.TInputContext;
import io.github.piscescup.fabricmc.carpetgui.gui.pages.RulePage;
import io.github.piscescup.fabricmc.carpetgui.gui.pages.AllRulesPage;
import io.github.piscescup.fabricmc.carpetgui.gui.GUIStyle;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.button.ChromeButton;
import io.github.piscescup.fabricmc.carpetgui.store.ModIconStore;
import io.github.piscescup.fabricmc.carpetgui.util.MsgUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/** Displays, selects and closes the pages owned by a {@link CarpetWorkspaceEditor}. */
public final class WorkspaceTabStripPanel extends TElement {
    private final CarpetWorkspaceEditor editor;

    public WorkspaceTabStripPanel(CarpetWorkspaceEditor editor) {
        this.editor = Objects.requireNonNull(editor, "editor");
        focusableProperty().set(false, WorkspaceTabStripPanel.class);
        hoverableProperty().set(false, WorkspaceTabStripPanel.class);
    }

    @Override
    protected void initCallback() {
        var bounds = getBounds();
        var panel = new TPanelElement.Paintable(0x35000000, 0, 0) {
            @Override
            public boolean inputCallback(TInputContext.InputDiscoveryPhase phase, TInputContext context) {
                if (phase == TInputContext.InputDiscoveryPhase.MAIN
                    && context.getInputType() == TInputContext.InputType.MOUSE_SCROLL) {
                    scroll((int) (context.getScrollY() * 35 - context.getScrollX() * 35), 0);
                    return true;
                }
                return super.inputCallback(phase, context);
            }
        };
        panel.setBounds(bounds);
        panel.scrollPaddingProperty().set(0, WorkspaceTabStripPanel.class);
        add(panel);

        int x = bounds.x;
        x += addEntry(panel, x, null, "carpet-gui", MsgUtils.tr("home"), 56, false);
        if (editor.isPageOpen(CarpetWorkspaceEditor.RULE_GROUPS_PAGE_ID)) {
            Component title = MsgUtils.tr("rule_groups");
            int width = tabWidth(title, 90);
            x += addEntry(panel, x, CarpetWorkspaceEditor.RULE_GROUPS_PAGE_ID,
                "carpet-gui", title, width, true);
        }
        for (RulePage page : editor.pages()) {
            if (!editor.isPageOpen(page.id())) continue;
            String iconId = page instanceof AllRulesPage ? "carpet-gui" : page.id();
            x += addEntry(panel, x, page.id(), iconId, page.title(), tabWidth(page.title(), 80), true);
        }

        for (TElement child : panel) {
            if (!(child instanceof EntryElement entry) || !entry.selected) continue;
            var entryBounds = entry.getBounds();
            if (entryBounds.endX > bounds.endX) {
                panel.scroll(bounds.endX - entryBounds.endX, 0);
            } else if (entryBounds.x < bounds.x) {
                panel.scroll(bounds.x - entryBounds.x, 0);
            }
            break;
        }
    }

    private int addEntry(
        TPanelElement panel,
        int x,
        String id,
        String iconId,
        Component title,
        int width,
        boolean closable
    ) {
        var entry = new EntryElement(id, iconId, title, closable);
        entry.setBounds(x, getBounds().y, width, getBounds().height);
        panel.add(entry);
        return width;
    }

    private int tabWidth(Component title, int minimum) {
        return Math.clamp(
            (int) Math.ceil(getClient().font.width(title) * GUIStyle.TEXT_SCALE) + 35,
            minimum,
            190
        );
    }

    private void addIcon(TElement parent, String modId, int x, int y, int size) {
        Identifier icon = "minecraft".equals(modId)
            ? Identifier.withDefaultNamespace("textures/block/grass_block_side.png")
            : ModIconStore.INSTANCE.icon(modId).orElse(null);
        if (icon != null) {
            var texture = new TTextureElement(icon);
            texture.modeProperty().set(TTextureElement.Mode.TEXTURE, WorkspaceTabStripPanel.class);
            texture.setBounds(x, y, size, size);
            parent.add(texture);
            return;
        }
        var fallback = WorkspaceStyle.label(
            parent, Component.literal(modId.substring(0, 1).toUpperCase()),
            x, y, size, size, GUIStyle.ACCENT_COLOR
        );
        fallback.textAlignmentProperty().set(CompassDirection.CENTER, WorkspaceTabStripPanel.class);
        fallback.textScaleProperty().set(Math.max(1.0, size / 15.0), WorkspaceTabStripPanel.class);
    }

    private final class EntryElement extends ChromeButton {
        private final String id;
        private final String iconId;
        private final boolean closable;
        private final boolean selected;

        private EntryElement(String id, String iconId, Component title, boolean closable) {
            super(title, () -> editor.selectPage(id));
            this.id = id;
            this.iconId = iconId;
            this.closable = closable;
            selected = editor.isSelected(id);
            setSelected(selected);
        }

        @Override
        protected void initCallback() {
            super.initCallback();
            var bounds = getBounds();
            getLabel().setBounds(
                bounds.x + 19, bounds.y + 2,
                Math.max(1, bounds.width - (closable ? 34 : 23)), bounds.height - 4
            );
            getLabel().textAlignmentProperty().set(CompassDirection.WEST, EntryElement.class);
            addIcon(this, iconId, bounds.x + 5, bounds.y + 4, 10);
            if (closable) {
                var close = new ChromeButton(Component.literal("×"), () -> editor.closePage(id));
                close.setBounds(bounds.endX - 15, bounds.y + 2, 13, bounds.height - 4);
                close.tooltipProperty().set(ignored -> TTooltip.of(MsgUtils.tr("close_tab")), EntryElement.class);
                add(close);
            }
            tooltipProperty().set(ignored -> TTooltip.of(getLabel().getText()), EntryElement.class);
        }
    }
}
//#endif

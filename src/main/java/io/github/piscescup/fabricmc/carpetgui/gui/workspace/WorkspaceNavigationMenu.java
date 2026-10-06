package io.github.piscescup.fabricmc.carpetgui.gui.workspace;

import com.thecsdev.common.util.enumerations.CompassDirection;
import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.ctxmenu.TContextMenu;
import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
import com.thecsdev.commonmc.api.client.gui.widget.TScrollBarWidget;
import io.github.piscescup.fabricmc.carpetgui.gui.render.PopupStyle;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Shared continuous-row popup for navigation, menus and value selection. Icons are optional. */
final class WorkspaceNavigationMenu extends TContextMenu {
    static final int ROW_HEIGHT = 22;
    static final int PADDING = 4;
    static final int SEPARATOR_HEIGHT = 6;
    private final List<Entry> entries;
    private final IconRenderer icons;

    record Entry(String iconId, Component title, boolean selected, boolean separatorAfter, Runnable action) {}

    @FunctionalInterface
    interface IconRenderer {
        void add(TElement parent, String iconId, int x, int y, int size);
    }

    WorkspaceNavigationMenu(List<Entry> entries, IconRenderer icons) {
        this.entries = List.copyOf(entries);
        this.icons = icons;
    }

    int contentHeight() {
        return entries.size() * ROW_HEIGHT + (int) entries.stream().filter(Entry::separatorAfter).count() * SEPARATOR_HEIGHT;
    }

    @Override protected void initCallback() {
        var bounds = getBounds();
        boolean overflow = contentHeight() > bounds.height - PADDING * 2;
        var list = new WorkspacePanel(0) {
            @Override public void renderCallback(TGuiGraphics graphics) {}
        };
        int width = Math.max(1, bounds.width - PADDING * 2 - (overflow ? 8 : 0));
        list.setBounds(bounds.x + PADDING, bounds.y + PADDING, width, Math.max(1, bounds.height - PADDING * 2));
        add(list);
        int y = bounds.y + PADDING;
        for (Entry entry : entries) {
            var item = new NavigationItem(entry);
            item.setBounds(bounds.x + PADDING, y, width, ROW_HEIGHT);
            list.add(item);
            y += ROW_HEIGHT;
            if (entry.separatorAfter()) {
                var separator = new TElement() {
                    @Override public void renderCallback(TGuiGraphics graphics) {
                        var line = getBounds();
                        graphics.fillColor(line.x, line.y + SEPARATOR_HEIGHT / 2, line.width, 1, PopupStyle.SEPARATOR);
                    }
                };
                separator.hoverableProperty().set(false, WorkspaceNavigationMenu.class);
                separator.focusableProperty().set(false, WorkspaceNavigationMenu.class);
                separator.setBounds(bounds.x + PADDING, y, width, SEPARATOR_HEIGHT);
                list.add(separator);
                y += SEPARATOR_HEIGHT;
            }
        }
        if (overflow) {
            var scrollbar = new TScrollBarWidget.Flat(list, TScrollBarWidget.ScrollDirection.VERTICAL);
            scrollbar.setBounds(bounds.endX - PADDING - 7, bounds.y + PADDING, 7, Math.max(1, bounds.height - PADDING * 2));
            add(scrollbar);
        }
    }

    @Override public void renderCallback(TGuiGraphics graphics) {
        var bounds = getBounds();
        PopupStyle.background(graphics, bounds.x, bounds.y, bounds.width, bounds.height);
    }

    private final class NavigationItem extends WorkspaceStyle.Button {
        NavigationItem(Entry entry) {
            super(entry.title(), () -> {
                WorkspaceNavigationMenu.this.remove();
                entry.action().run();
            });
            setSelected(entry.selected());
            this.entry = entry;
        }

        private final Entry entry;

        @Override protected void initCallback() {
            super.initCallback();
            var bounds = getBounds();
            boolean hasIcon = entry.iconId() != null && icons != null;
            int inset = hasIcon ? 21 : 6;
            getLabel().setBounds(bounds.x + inset, bounds.y, Math.max(1, bounds.width - inset - 4), bounds.height);
            getLabel().textAlignmentProperty().set(CompassDirection.WEST, NavigationItem.class);
            getLabel().wrapTextProperty().set(false, NavigationItem.class);
            if (hasIcon) icons.add(this, entry.iconId(), bounds.x + 3, bounds.y + 5, 12);
        }

        @Override public void renderCallback(TGuiGraphics graphics) {
            if (!isHoveredOrFocused()) return;
            var bounds = getBounds();
            graphics.fillColor(bounds.x, bounds.y, bounds.width, bounds.height, PopupStyle.HOVER);
        }
    }
}

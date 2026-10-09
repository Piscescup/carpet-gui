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

package io.github.piscescup.fabricmc.carpetgui.gui.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Selection, keyboard cursor and scrolling, independent of the renderer.
 */
public final class DropdownState<T extends DropdownOption> {
    private final List<T> options;
    private final Consumer<T> onSelection;
    private int selected;
    private int highlighted;
    private int firstVisible;
    private int visibleRows;
    private boolean open;

    public DropdownState(List<? extends T> options, String selectedId, Consumer<T> onSelection) {
        this.options = List.copyOf(options);
        this.onSelection = Objects.requireNonNull(onSelection);
        if (this.options.isEmpty()) throw new IllegalArgumentException("Dropdown needs at least one option");
        var ids = new HashSet<String>();
        int initial = -1;
        for (int i = 0; i < this.options.size(); i++) {
            String id = Objects.requireNonNull(this.options.get(i)
                .id());
            if (!ids.add(id)) throw new IllegalArgumentException("Duplicate dropdown ID: " + id);
            if (id.equals(selectedId)) initial = i;
        }
        if (initial < 0) throw new IllegalArgumentException("Unknown dropdown ID: " + selectedId);
        selected = highlighted = initial;
        setVisibleRows(8);
    }

    public List<T> options() {
        return options;
    }

    public T selected() {
        return options.get(selected);
    }

    public int selectedIndex() {
        return selected;
    }

    public int highlightedIndex() {
        return highlighted;
    }

    public int firstVisible() {
        return firstVisible;
    }

    public int visibleRows() {
        return visibleRows;
    }

    public boolean isOpen() {
        return open;
    }

    public void setVisibleRows(int rows) {
        visibleRows = Math.clamp(rows, 1, options.size());
        firstVisible = Math.clamp(firstVisible, 0, options.size() - visibleRows);
        ensureHighlightVisible();
    }

    public void open() {
        open = true;
        highlight(selected);
    }

    public void close() {
        open = false;
    }

    public void highlight(int index) {
        highlighted = Math.clamp(index, 0, options.size() - 1);
        ensureHighlightVisible();
    }

    public void move(int delta) {
        highlight(highlighted + delta);
    }

    public void scrollBy(int rows) {
        firstVisible = Math.clamp(firstVisible + rows, 0, options.size() - visibleRows);
    }

    public void select(int index) {
        if (index < 0 || index >= options.size()) throw new IllegalArgumentException("Invalid dropdown index");
        close();
        highlight(index);
        if (selected == index) return;
        selected = index;
        onSelection.accept(selected());
    }

    public void commit() {
        select(highlighted);
    }

    /**
     * Reflect a backend update without treating it as a user selection.
     */
    public void synchronizeSelection(String id) {
        for (int index = 0; index < options.size(); index++) {
            if (options.get(index)
                .id()
                .equals(id)) {
                selected = index;
                if (!open) highlight(index);
                return;
            }
        }
    }

    private void ensureHighlightVisible() {
        if (highlighted < firstVisible) firstVisible = highlighted;
        if (highlighted >= firstVisible + visibleRows) firstVisible = highlighted - visibleRows + 1;
    }
}

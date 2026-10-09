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

package io.github.piscescup.fabricmc.carpetgui.gui.layout;

/**
 * Menu geometry with a scrollable row limit. The usable area must fit at least one row.
 */
public record DropdownLayout(
    GuiBounds bounds,
    int rows
) {
    public static final int ROW_HEIGHT = 22;

    public static DropdownLayout below(GuiBounds anchor, GuiBounds area, int optionCount) {
        int top = Math.clamp(anchor.bottom() + 2, area.top(), Math.max(area.top(), area.bottom() - 26));
        int rows = Math.clamp(optionCount, 1, Math.clamp((area.bottom() - top - 4) / ROW_HEIGHT, 1, 8));
        int width = Math.min(anchor.width(), area.width());
        int left = Math.clamp(anchor.left(), area.left(), area.right() - width);
        return new DropdownLayout(new GuiBounds(left, top, width, rows * ROW_HEIGHT + 4), rows);
    }

    public int rowAt(double x, double y) {
        if (!bounds.contains(x, y) || y < bounds.top() + 2 || y >= bounds.bottom() - 2) return -1;
        return (int) ((y - bounds.top() - 2) / ROW_HEIGHT);
    }
}

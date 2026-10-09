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
 * Explicit x/y/width/height geometry, rather than Minecraft's ambiguous four-int overload.
 */
public record GuiBounds(
    int left,
    int top,
    int width,
    int height
) {
    public GuiBounds {
        if (width < 0 || height < 0) throw new IllegalArgumentException("Negative GUI size");
    }

    public int right() {
        return left + width;
    }

    public int bottom() {
        return top + height;
    }

    public boolean contains(double x, double y) {
        return x >= left && x < right() && y >= top && y < bottom();
    }

    public boolean intersects(int y, int height) {
        return y + height > top && y < bottom();
    }

    public boolean encloses(GuiBounds other) {
        return other.left >= left && other.top >= top && other.right() <= right() && other.bottom() <= bottom();
    }
}

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
 * Shared columns for every rule, independent of scroll and category.
 */
public record RuleRowLayout(
    int labelLeft,
    int labelWidth,
    GuiBounds value,
    GuiBounds reset,
    GuiBounds saveDefault
) {
    public static RuleRowLayout at(int screenWidth, int rowY) {
        return at(screenWidth, rowY, false);
    }

    public static RuleRowLayout at(int screenWidth, int rowY, boolean persistent) {
        int labelLeft = 38;
        int valueLeft = Math.max(130, screenWidth * 23 / 100);
        int resetWidth = Math.clamp(screenWidth / 10, 36, 58);
        int defaultWidth = persistent ? Math.clamp(screenWidth / 8, 74, 90) : 0;
        int valueWidth = Math.max(
            1,
            Math.min(
                260,
                Math.min(screenWidth * 19 / 100, screenWidth - valueLeft - resetWidth - defaultWidth - (persistent ? 40 : 32))
            )
        );

        return new RuleRowLayout(
            labelLeft, Math.max(1, valueLeft - labelLeft - 10),
            new GuiBounds(valueLeft, rowY + 2, valueWidth, 20),
            new GuiBounds(valueLeft + valueWidth + 8, rowY + 2, resetWidth, 20),
            new GuiBounds(valueLeft + valueWidth + resetWidth + 16, rowY + 2, defaultWidth, 20)
        );
    }
}

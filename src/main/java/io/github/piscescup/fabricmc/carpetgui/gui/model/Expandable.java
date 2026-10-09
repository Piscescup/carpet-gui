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

/**
 * Shared by an individual [+]/[-] toggle and bulk expansion actions.
 */
public interface Expandable {
    boolean isExpanded();

    void setExpanded(boolean expanded);

    default void toggle() {
        setExpanded(!isExpanded());
    }

    static void setAll(Iterable<? extends Expandable> sections, boolean expanded) {
        for (Expandable section : sections) section.setExpanded(expanded);
    }
}

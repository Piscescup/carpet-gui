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

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Generic section state and animation; independent of Minecraft rendering and rule data.
 */
public final class CollapsibleSection<T>
    implements Expandable
{
    private static final double ANIMATION_SECONDS = 0.16;
    private final String id;
    private final List<T> items;
    private final Consumer<Boolean> onChange;
    private boolean expanded;
    private double progress;

    public CollapsibleSection(String id, List<T> items, boolean initiallyExpanded, Consumer<Boolean> onChange) {
        this.id = Objects.requireNonNull(id);
        this.items = List.copyOf(items);
        this.onChange = Objects.requireNonNull(onChange);
        expanded = initiallyExpanded;
        progress = expanded ? 1 : 0;
    }

    public String id() {
        return id;
    }

    public List<T> items() {
        return items;
    }

    @Override
    public boolean isExpanded() {
        return expanded;
    }

    @Override
    public void setExpanded(boolean expanded) {
        if (this.expanded == expanded) return;
        this.expanded = expanded;
        onChange.accept(expanded);
    }

    public void advance(double seconds) {
        double step = Math.max(0, seconds) / ANIMATION_SECONDS;
        progress = Math.clamp(progress + (expanded ? step : -step), 0, 1);
    }

    public double openFraction() {
        return progress * progress * (3 - 2 * progress);
    }

    public int bodyHeight(int rowHeight) {
        return (int) Math.round(items.size() * rowHeight * openFraction());
    }
}

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
package io.github.piscescup.fabricmc.carpetgui.gui.menubar;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Ordered menu-entry registry, following BetterStats' extensible menubar structure. */
public final class MenubarEntryRegistry {
    private static final Map<String, MenubarEntry> ENTRIES = new LinkedHashMap<>();

    static {
        register("file", FileMenubarEntry.INSTANCE);
        register("view", ViewMenubarEntry.INSTANCE);
        register("about", AboutMenubarEntry.INSTANCE);
    }

    private MenubarEntryRegistry() {}

    public static synchronized void register(String id, MenubarEntry entry) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(entry, "entry");
        if (ENTRIES.putIfAbsent(id, entry) != null) {
            throw new IllegalArgumentException("A menubar entry is already registered as " + id);
        }
    }

    public static synchronized List<MenubarEntry> entries() {
        return List.copyOf(ENTRIES.values());
    }
}
//#endif

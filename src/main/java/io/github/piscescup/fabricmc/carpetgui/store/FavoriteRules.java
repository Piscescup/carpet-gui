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

package io.github.piscescup.fabricmc.carpetgui.store;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.Set;

import static io.github.piscescup.fabricmc.carpetgui.References.LOGGER;

/** Client-local persistent favorites, keyed by a rule's stable state ID. */
public final class FavoriteRules {
    private static final Set<String> RULE_IDS = new LinkedHashSet<>();
    private static boolean loaded;

    private FavoriteRules() {
    }

    public static synchronized boolean contains(String ruleId) {
        load();
        return RULE_IDS.contains(ruleId);
    }

    public static synchronized boolean toggle(String ruleId) {
        load();
        boolean favorite;
        if (RULE_IDS.remove(ruleId)) {
            favorite = false;
        } else {
            RULE_IDS.add(ruleId);
            favorite = true;
        }
        save();
        return favorite;
    }

    public static void load() {
        if (loaded) return;
        loaded = true;
        Path file = file();
        if (!Files.isRegularFile(file)) return;
        try {
            Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                .map(String::strip)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .forEach(RULE_IDS::add);
        } catch (IOException failure) {
            LOGGER.warn("Cannot read Carpet GUI favorites from {}", file, failure);
        }
    }

    public static void save() {
        Path file = file();
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            Files.write(temporary, RULE_IDS.stream().sorted().toList(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException failure) {
            LOGGER.warn("Cannot save Carpet GUI favorites to {}", file, failure);
        }
    }

    public static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("carpet-gui").resolve("favorites.txt");
    }
}

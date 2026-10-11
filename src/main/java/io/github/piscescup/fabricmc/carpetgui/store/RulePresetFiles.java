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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Restricts in-game file access to individual JSON files in the presets directory. */
public final class RulePresetFiles {
    private RulePresetFiles() {}

    public static Path directory() { return RuleGroupStore.directory().resolve("presets"); }

    public static List<String> list(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) return List.of();
        try (var files = Files.list(directory)) {
            return files.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                .map(path -> path.getFileName().toString()).filter(name -> name.endsWith(".json"))
                .sorted(String.CASE_INSENSITIVE_ORDER).limit(200).toList();
        }
    }

    public static Path resolve(Path directory, String name) {
        if (name == null || !name.matches("[\\p{L}\\p{N}_. -]{1,100}\\.json") || name.endsWith(" .json")
            || name.toUpperCase(Locale.ROOT).matches("(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])\\..*")) {
            throw new IllegalArgumentException("Invalid preset filename");
        }
        Path root = directory.toAbsolutePath().normalize();
        Path path = root.resolve(name).normalize();
        if (!path.getParent().equals(root)) throw new IllegalArgumentException("Invalid preset path");
        return path;
    }

    public static RulePreset read(Path directory, String name) throws IOException {
        Path file = resolve(directory, name);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Not a preset file");
        try (var input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes = input.readNBytes(RulePreset.MAX_BYTES + 1);
            if (bytes.length > RulePreset.MAX_BYTES) throw new IOException("Preset is too large");
            return RulePreset.decode(StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString());
        }
    }

    public static Path write(Path directory, RulePreset preset) throws IOException {
        String json = preset.encode();
        Files.createDirectories(directory);
        Path file = resolve(directory, "preset-" + UUID.randomUUID() + ".json");
        Files.writeString(file, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        return file;
    }
}

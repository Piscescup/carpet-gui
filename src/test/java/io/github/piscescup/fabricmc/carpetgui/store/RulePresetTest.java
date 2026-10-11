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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RulePresetTest {
    @TempDir Path directory;
    private RulePreset preset() { return new RulePreset("pry/测试", "building", Map.of("pry::pry:test", "true")); }

    @Test void roundTripPreservesUnicodePathsAndValues() {
        assertEquals(preset(), RulePreset.decode(preset().encode()));
        assertFalse(preset().encode().contains("author"));
        assertFalse(preset().encode().contains("pushedRevision"));
    }

    @Test void rejectUnknownVersionsAndNonStringValues() {
        assertThrows(IllegalArgumentException.class, () -> RulePreset.decode(preset().encode().replace("\"version\": 1", "\"version\": 2")));
        assertThrows(IllegalArgumentException.class, () -> RulePreset.decode("{\"format\":\"carpet-gui-preset\",\"version\":1,\"name\":\"x\",\"rules\":{\"r\":true}}"));
    }

    @Test void rejectDuplicateKeysAndTrailingDocuments() {
        assertThrows(IllegalArgumentException.class, () -> RulePreset.decode("{\"format\":\"carpet-gui-preset\",\"version\":1,\"name\":\"x\",\"rules\":{\"r\":\"1\",\"r\":\"2\"}}"));
        assertThrows(IllegalArgumentException.class, () -> RulePreset.decode(preset().encode() + "{}"));
        assertThrows(IllegalArgumentException.class, () -> RulePreset.decode(preset().encode().replace("\"version\": 1", "\"version\": 1, \"version\": 1")));
    }

    @Test void rejectAmbiguousRuleAliasesAndCommandControlCharacters() {
        assertThrows(IllegalArgumentException.class, () -> new RulePreset("x", "", Map.of("r", "1", "page::r", "2")));
        assertThrows(IllegalArgumentException.class, () -> new RulePreset("x", "", Map.of("r", "true\n/carpet x false")));
        assertThrows(IllegalArgumentException.class, () -> new RulePreset("x\ny", "", Map.of("r", "true")));
        assertThrows(IllegalArgumentException.class, () -> new RulePreset("x", "tag\t", Map.of("r", "true")));
        assertThrows(IllegalArgumentException.class, () -> new RulePreset("x", "", Map.of()));
    }

    @Test void exportNeverOverwritesAndImportRoundTrips() throws Exception {
        Path first = RulePresetFiles.write(directory, preset());
        Path second = RulePresetFiles.write(directory, preset());
        assertNotEquals(first, second);
        assertEquals(preset(), RulePresetFiles.read(directory, first.getFileName().toString()));
        assertEquals(2, RulePresetFiles.list(directory).size());
    }

    @Test void pathsCannotEscapePresetDirectory() {
        for (String name : new String[]{"../outside.json", "..\\outside.json", "C:/outside.json", "CON.json", "a.json:stream", "NUL.json"}) {
            assertThrows(IllegalArgumentException.class, () -> RulePresetFiles.resolve(directory, name), name);
        }
    }

    @Test void oversizedAndInvalidUtf8FilesAreRejected() throws Exception {
        Files.write(directory.resolve("large.json"), new byte[RulePreset.MAX_BYTES + 1]);
        assertThrows(java.io.IOException.class, () -> RulePresetFiles.read(directory, "large.json"));
        Files.write(directory.resolve("invalid.json"), new byte[]{(byte) 0xC3, (byte) 0x28});
        assertThrows(java.io.IOException.class, () -> RulePresetFiles.read(directory, "invalid.json"));
    }

    @Test void importedGroupsAreNewLocalPendingSnapshots() {
        var first = RuleGroupStore.presetGroup(preset(), "Tester", 1);
        var second = RuleGroupStore.presetGroup(preset(), "Tester", 2);
        assertNotEquals(first.id(), second.id());
        assertEquals(preset().rules(), first.head());
        assertEquals(1, first.commitsAhead());
        assertEquals(0, first.pushedRevision());
        assertFalse(first.hasStagedChanges());
        assertEquals("Tester", first.history().getFirst().author());
    }
}

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

import static org.junit.jupiter.api.Assertions.*;

class WorkspaceContextTest {
    @TempDir Path directory;

    @Test void canonicalServerAddressIsIndependentOfCaseAndDefaultPortNotation() {
        assertEquals(WorkspaceContext.server(" EXAMPLE.COM. ", 25565), WorkspaceContext.server("example.com", 25565));
        assertNotEquals(WorkspaceContext.server("example.com", 25565).id(), WorkspaceContext.server("example.com", 25566).id());
        assertNotEquals(WorkspaceContext.server("a.invalid", 25565).id(), WorkspaceContext.server("b.invalid", 25565).id());
    }

    @Test void multiplayerFolderUsesRequestedPrefixAndSafeAddress() {
        Path multiplayerRoot = directory.resolve("carpet-gui");
        Path file = WorkspaceContext.server("[2001:db8::1]", 25565).file(directory.resolve("config/carpet-gui"), multiplayerRoot);
        String folder = file.getParent().getFileName().toString();
        assertTrue(folder.startsWith("Multiplayergames_"));
        assertTrue(folder.matches("Multiplayergames_[a-zA-Z0-9._-]+_[0-9a-f]{64}"));
        assertEquals(multiplayerRoot, file.getParent().getParent());
        assertEquals(directory.resolve("config").getParent(), multiplayerRoot.getParent());
        assertEquals("rule-groups.json", file.getFileName().toString());
    }

    @Test void worldStateLivesAtSaveRootInsteadOfGlobalConfig() throws Exception {
        Path save = Files.createDirectories(directory.resolve("saves/Test"));
        var context = WorkspaceContext.world(save.resolve("."));
        assertEquals(WorkspaceContext.world(save), context);
        assertEquals(save.toRealPath().resolve("carpet-gui/rule-groups.json"), context.file(directory.resolve("config"), directory.resolve("carpet-gui")));
        assertNotEquals(context.id(), WorkspaceContext.world(directory.resolve("other-save")).id());
    }

    @Test void offlineAndMalformedContextCannotEscapeStorage() {
        assertEquals(directory.resolve("offline/rule-groups.json"), WorkspaceContext.offline().file(directory, directory.resolve("carpet-gui")));
        assertFalse(WorkspaceContext.offline().connected());
        assertThrows(IllegalArgumentException.class, () -> new WorkspaceContext("../../outside", "x", null));
        assertThrows(IllegalArgumentException.class, () -> WorkspaceContext.server("example.com", 0));
    }

    @Test void reconnectAndDisconnectInvalidatePreviousTokens() {
        try {
            var context = WorkspaceContext.server("example.com", 25565);
            WorkspaceSession.begin(context);
            var old = WorkspaceSession.current();
            WorkspaceSession.begin(context);
            assertFalse(old.canApply());
            assertTrue(WorkspaceSession.current().canApply());
            WorkspaceSession.begin(WorkspaceContext.offline());
            assertFalse(WorkspaceSession.current().canApply());
        } finally { WorkspaceSession.begin(WorkspaceContext.offline()); }
    }
}

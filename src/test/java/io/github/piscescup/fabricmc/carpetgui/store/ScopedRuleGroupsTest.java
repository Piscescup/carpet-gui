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

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScopedRuleGroupsTest {
    @TempDir Path directory;

    private RuleGroupStore.Group group(String name) {
        return RuleGroupStore.presetGroup(new RulePreset(name, "", Map.of("rule", "true")), "Tester", 1);
    }

    @Test void serversPersistSeparateStagingHistoryAndPushProgressAcrossRestart() {
        var a = WorkspaceContext.server("a.invalid", 25565);
        var b = WorkspaceContext.server("b.invalid", 25565);
        var repository = new RuleGroupStore.Repository(directory, directory.resolve("multiplayer"));
        repository.switchContext(a);
        var first = group("A");
        first.acknowledge(1);
        first.stage("rule", "false");
        repository.groups().add(first);
        repository.switchContext(b);
        assertTrue(repository.groups().isEmpty());
        repository.groups().add(group("B"));
        assertTrue(repository.save());
        repository.switchContext(a);
        var restored = repository.groups().getFirst();
        assertNotSame(first, restored);
        assertEquals("A", restored.name());
        assertEquals(Map.of("rule", "false"), restored.staged());
        assertEquals(1, restored.pushedRevision());
        assertEquals(1, restored.history().size());
        var restarted = new RuleGroupStore.Repository(directory, directory.resolve("multiplayer"));
        restarted.switchContext(b);
        assertEquals("B", restarted.groups().getFirst().name());
        assertEquals(0, restarted.groups().getFirst().pushedRevision());
    }

    @Test void multiplayerOnlyUsesInstanceRootWithoutReadingPreviousConfigLocation() throws Exception {
        Path globalRoot = directory.resolve("config/carpet-gui");
        Path multiplayerRoot = directory.resolve("carpet-gui");
        var context = WorkspaceContext.server("example.com", 25565);
        Path previousFile = context.file(globalRoot, globalRoot);
        Files.createDirectories(previousFile.getParent());
        String previous = "{\"format\":4,\"groups\":[" + new Gson().toJson(group("Previous location")) + "]}";
        Files.writeString(previousFile, previous);
        var repository = new RuleGroupStore.Repository(globalRoot, multiplayerRoot);
        repository.switchContext(context);
        assertTrue(repository.groups().isEmpty());
        assertEquals(multiplayerRoot, repository.file().getParent().getParent());
        repository.groups().add(group("New location"));
        assertTrue(repository.save());
        assertTrue(Files.isRegularFile(context.file(globalRoot, multiplayerRoot)));
        assertEquals(previous, Files.readString(previousFile));
    }

    @Test void saveRootsAndOfflineStateNeverMix() throws Exception {
        var repository = new RuleGroupStore.Repository(directory.resolve("config"), directory.resolve("multiplayer"));
        var worldA = WorkspaceContext.world(Files.createDirectory(directory.resolve("world-a")));
        var worldB = WorkspaceContext.world(Files.createDirectory(directory.resolve("world-b")));
        repository.switchContext(worldA);
        repository.groups().add(group("World A"));
        repository.switchContext(worldB);
        assertTrue(repository.groups().isEmpty());
        repository.groups().add(group("World B"));
        repository.switchContext(WorkspaceContext.offline());
        assertTrue(repository.groups().isEmpty());
        assertTrue(Files.isRegularFile(directory.resolve("world-a/carpet-gui/rule-groups.json")));
        assertTrue(Files.isRegularFile(directory.resolve("world-b/carpet-gui/rule-groups.json")));
        repository.switchContext(worldA);
        assertEquals("World A", repository.groups().getFirst().name());
    }

    @Test void legacyCopyIsExplicitOncePerEnvironmentAndNeverInheritsPushedStatus() throws Exception {
        var original = group("Old");
        original.acknowledge(1);
        original.stage("rule", "false");
        original.addMember("untracked");
        String legacy = "{\"format\":4,\"groups\":[" + new Gson().toJson(original) + "]}";
        Path file = directory.resolve("rule-groups.json");
        Files.writeString(file, legacy);
        var repository = new RuleGroupStore.Repository(directory, directory.resolve("multiplayer"));
        var context = WorkspaceContext.server("a.invalid", 25565);
        repository.switchContext(context);
        assertTrue(repository.groups().isEmpty());
        assertTrue(repository.legacyAvailable());
        assertEquals(1, repository.copyLegacyGroups("Migrator", 2));
        var copy = repository.groups().getFirst();
        assertNotEquals(original.id(), copy.id());
        assertEquals(0, copy.pushedRevision());
        assertEquals(1, copy.commitsAhead());
        assertEquals(original.head(), copy.head());
        assertEquals(original.staged(), copy.staged());
        assertEquals(original.members(), copy.members());
        assertEquals("Migrator", copy.history().getFirst().author());
        assertEquals(legacy, Files.readString(file));
        assertEquals(0, repository.copyLegacyGroups("Migrator", 3));
        var restarted = new RuleGroupStore.Repository(directory, directory.resolve("multiplayer"));
        restarted.switchContext(context);
        assertFalse(restarted.legacyAvailable());
        restarted.switchContext(WorkspaceContext.server("b.invalid", 25565));
        assertTrue(restarted.legacyAvailable());
    }

    @Test void corruptContextFileIsNotOverwrittenAndInMemoryEditsSurviveSwitching() throws Exception {
        var context = WorkspaceContext.server("a.invalid", 25565);
        Path file = context.file(directory, directory.resolve("multiplayer"));
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{broken");
        var repository = new RuleGroupStore.Repository(directory, directory.resolve("multiplayer"));
        repository.switchContext(context);
        repository.groups().add(group("Unsaved"));
        assertTrue(repository.storageProblem());
        assertFalse(repository.save());
        repository.switchContext(WorkspaceContext.offline());
        repository.switchContext(context);
        assertEquals("Unsaved", repository.groups().getFirst().name());
        assertFalse(repository.save());
        assertEquals("{broken", Files.readString(file));
    }

    @Test void failedSaveRetainsUnsavedDataUntilReturningToEnvironment() throws Exception {
        Path root = directory.resolve("blocked");
        Files.writeString(root, "not a directory");
        var repository = new RuleGroupStore.Repository(root, root);
        var context = WorkspaceContext.server("a.invalid", 25565);
        repository.switchContext(context);
        repository.groups().add(group("Pending"));
        repository.switchContext(WorkspaceContext.server("b.invalid", 25565));
        assertTrue(repository.groups().isEmpty());
        Files.delete(root);
        repository.switchContext(context);
        assertEquals("Pending", repository.groups().getFirst().name());
        assertTrue(repository.save());
        assertTrue(Files.isRegularFile(context.file(root, root)));
        assertFalse(repository.storageProblem());
    }
}

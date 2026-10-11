/*
 * This file is part of the Carpet GUI project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  PiscesCup and contributors
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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RuleGroupStoreTest {
    private static RuleGroupStore.Group committed() {
        var group = new RuleGroupStore.Group();
        group.addMember("rule");
        group.stage("rule", "true");
        assertTrue(group.commit("initial", "Tester", 1));
        return group;
    }

    @Test
    void removalDoesNotMutateHeadUntilCommitted() {
        var group = committed();
        group.removeMember("rule");
        assertEquals("true", group.headValue("rule"));
        assertEquals(1, group.revision());
        assertTrue(group.stagedDeletions().contains("rule"));
        assertTrue(group.commit("remove", "Tester", 2));
        assertFalse(group.head().containsKey("rule"));
        assertEquals(Map.of("rule", "true"), group.history().getFirst().previousValues());
    }

    @Test
    void readdingCancelsPendingDeletion() {
        var group = committed();
        group.removeMember("rule");
        group.addMember("rule");
        assertFalse(group.hasStagedChanges());
        assertEquals("true", group.headValue("rule"));
    }

    @Test
    void pushingOldRevisionDoesNotAcknowledgeNewCommit() {
        var group = committed();
        group.stage("rule", "false");
        group.commit("second", "Tester", 2);
        group.acknowledge(1);
        assertEquals(1, group.commitsAhead());
        group.acknowledge(0);
        assertEquals(1, group.pushedRevision());
    }

    @Test
    void oldestRetainedCommitKeepsItsParentAfterFortyCommits() {
        var group = committed();
        for (int revision = 2; revision <= 45; revision++) {
            group.stage("rule", String.valueOf(revision));
            group.commit("r" + revision, "Tester", revision);
        }
        assertEquals(40, group.history().size());
        var oldest = group.history().getLast();
        assertEquals(6, oldest.revision());
        assertEquals(Map.of("rule", "5"), oldest.previousValues());
    }

    @Test
    void serializedPendingRemovalSurvivesSanitization() {
        var gson = new Gson();
        var group = committed();
        group.removeMember("rule");
        String json = "{\"format\":4,\"groups\":[" + gson.toJson(group) + "]}";
        var data = gson.fromJson(json, RuleGroupStore.Data.class);
        data.sanitize();
        var restored = gson.fromJson(gson.toJsonTree(data).getAsJsonObject().getAsJsonArray("groups").get(0), RuleGroupStore.Group.class);
        assertFalse(restored.members().contains("rule"));
        assertEquals("true", restored.headValue("rule"));
        assertTrue(restored.stagedDeletions().contains("rule"));
    }

    @Test
    void formatThreeSilentRemovalIsMigratedToStagedDeletion() {
        var gson = new Gson();
        var tree = gson.toJsonTree(committed()).getAsJsonObject();
        tree.add("members", gson.toJsonTree(new String[0]));
        tree.add("head", gson.toJsonTree(Map.of()));
        var data = gson.fromJson("{\"format\":3,\"groups\":[" + tree + "]}", RuleGroupStore.Data.class);
        data.sanitize();
        var restored = gson.fromJson(gson.toJsonTree(data).getAsJsonObject().getAsJsonArray("groups").get(0), RuleGroupStore.Group.class);
        assertEquals("true", restored.headValue("rule"));
        assertTrue(restored.stagedDeletions().contains("rule"));
    }
}

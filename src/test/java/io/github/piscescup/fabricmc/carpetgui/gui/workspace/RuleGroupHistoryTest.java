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

package io.github.piscescup.fabricmc.carpetgui.gui.workspace;

import com.google.gson.Gson;
import io.github.piscescup.fabricmc.carpetgui.store.RuleGroupStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleGroupHistoryTest {
    private static RuleGroupStore.Commit commit(String json) {
        return new Gson().fromJson(json, RuleGroupStore.Commit.class);
    }

    @Test
    void retainedParentProducesExactDiffEvenWithoutPreviousCommit() {
        var commit = commit("""
            {"revision":7,"values":{"stable":"true","changed":"new"},
             "previousValues":{"stable":"true","changed":"old","deleted":"value"}}
            """);
        var changes = RuleGroupHistory.changes(commit, null);
        assertTrue(RuleGroupHistory.hasBaseline(commit, null));
        assertEquals(2, changes.size());
        assertEquals("old", changes.getFirst().oldValue());
        assertNull(changes.getLast().newValue());
    }

    @Test
    void legacyTruncatedHistoryIsExplicitlyNotAnExactDiff() {
        var commit = commit("{\"revision\":7,\"values\":{\"rule\":\"true\"}}");
        assertFalse(RuleGroupHistory.hasBaseline(commit, null));
        assertEquals(1, RuleGroupHistory.changes(commit, null).size());
    }

    @Test
    void legacyFirstCommitAndAvailableParentStillWork() {
        var first = commit("{\"revision\":1,\"values\":{\"rule\":\"true\"}}");
        var second = commit("{\"revision\":2,\"values\":{\"rule\":\"false\"}}");
        assertTrue(RuleGroupHistory.hasBaseline(first, null));
        assertTrue(RuleGroupHistory.hasBaseline(second, first));
        assertEquals("true", RuleGroupHistory.changes(second, first).getFirst().oldValue());
    }
}

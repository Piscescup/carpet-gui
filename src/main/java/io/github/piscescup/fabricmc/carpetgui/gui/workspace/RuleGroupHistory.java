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

package io.github.piscescup.fabricmc.carpetgui.gui.workspace;

import io.github.piscescup.fabricmc.carpetgui.store.RuleGroupStore;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Version-independent commit comparison, including legacy commits with missing parents. */
final class RuleGroupHistory {
    private RuleGroupHistory() {}

    static boolean hasBaseline(RuleGroupStore.Commit commit, RuleGroupStore.Commit previous) {
        return commit.previousValues() != null || previous != null || commit.revision() <= 1;
    }

    static List<Change> changes(RuleGroupStore.Commit commit, RuleGroupStore.Commit previous) {
        Map<String, String> older = commit.previousValues() != null ? commit.previousValues()
            : previous != null ? previous.values() : Map.of();
        var ids = new LinkedHashSet<>(commit.values().keySet());
        ids.addAll(older.keySet());
        return ids.stream().filter(id -> !Objects.equals(older.get(id), commit.values().get(id)))
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .map(id -> new Change(id, older.get(id), commit.values().get(id))).toList();
    }

    record Change(String ruleId, String oldValue, String newValue) {}
}

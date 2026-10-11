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

import io.github.piscescup.fabricmc.carpetgui.api.EditableRuleView;
import io.github.piscescup.fabricmc.carpetgui.api.PersistentRuleEditor;
import io.github.piscescup.fabricmc.carpetgui.api.RuleEditor;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import io.github.piscescup.fabricmc.carpetgui.store.WorkspaceContext;
import io.github.piscescup.fabricmc.carpetgui.store.WorkspaceSession;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RuleApplyPlanTest {
    @BeforeEach void connect() { WorkspaceSession.begin(WorkspaceContext.server("test.invalid", 25565)); }
    @AfterEach void disconnect() { WorkspaceSession.begin(WorkspaceContext.offline()); }

    @Test void reconnectingInvalidatesPreviewEvenWithIdenticalValues() {
        var rule = new FakeRule();
        var plan = RuleApplyPlan.capture(1, Map.of("a", "true"), key -> rule);
        WorkspaceSession.begin(WorkspaceContext.server("test.invalid", 25565));
        assertFalse(plan.matches(1, Map.of("a", "true")));
    }

    @Test void offlinePreviewCannotApplyEvenClientEditableRules() {
        WorkspaceSession.begin(WorkspaceContext.offline());
        var plan = RuleApplyPlan.capture(1, Map.of("a", "true"), key -> new FakeRule());
        assertFalse(plan.entries().getFirst().applicable());
    }
    @Test void captureIsReadOnlyAndIncludesMissingAndBlockedRules() {
        var rule = new FakeRule();
        rule.writable = false;
        var plan = RuleApplyPlan.capture(1, Map.of("a", "true", "missing", "false"), key -> key.equals("a") ? rule : null);
        assertEquals(2, plan.entries().size());
        assertTrue(plan.entries().stream().noneMatch(RuleApplyPlan.Entry::applicable));
        assertEquals(0, rule.writes);
    }

    @Test void valuesAndConfigurationChangesInvalidatePreview() {
        var rule = new FakeRule();
        var plan = RuleApplyPlan.capture(1, Map.of("a", "true"), key -> rule);
        var entry = plan.entries().getFirst();
        assertTrue(entry.stillCurrent(rule));
        rule.saved = "false";
        assertFalse(entry.stillCurrent(rule));
        rule.saved = null;
        rule.value = "true";
        assertFalse(entry.stillCurrent(rule));
        assertFalse(plan.matches(2, plan.targets()));
        assertFalse(plan.matches(1, Map.of("a", "false")));
    }

    @Test void strictInvalidTargetsAndUnavailablePersistenceAreBlocked() {
        var rule = new FakeRule();
        rule.strict = true;
        assertFalse(RuleApplyPlan.capture(1, Map.of("a", "not-a-bool"), key -> rule).entries().getFirst().applicable());
        rule.saveAllowed = false;
        assertFalse(RuleApplyPlan.capture(1, Map.of("a", "true"), key -> rule).entries().getFirst().applicable());
    }

    @Test void unchangedRuntimeStillRequiresExplicitSavingAndSnapshotIsImmutable() {
        var rule = new FakeRule();
        var plan = RuleApplyPlan.capture(1, Map.of("a", "false"), key -> rule);
        assertTrue(plan.entries().getFirst().unchanged());
        assertTrue(plan.entries().getFirst().applicable());
        assertThrows(UnsupportedOperationException.class, () -> plan.targets().put("b", "true"));
    }

    static class FakeRule implements EditableRuleView, PersistentRuleEditor {
        String value = "false", saved;
        boolean writable = true, saveAllowed = true, strict;
        int writes;
        public String id() { return "a"; }
        public Component label() { return Component.literal("Test rule"); }
        public Component description() { return Component.empty(); }
        public String value() { return value; }
        public String defaultValue() { return "false"; }
        public Optional<String> configuredValue() { return Optional.ofNullable(saved); }
        public boolean explicitlyConfigured() { return saved != null; }
        public List<String> categories() { return List.of(); }
        public RuleEditor editor() { return this; }
        public InputKind inputKind() { return InputKind.BOOLEAN; }
        public List<String> suggestions() { return List.of("true", "false"); }
        public boolean strict() { return strict; }
        public boolean editable() { return writable; }
        public Component disabledReason() { return Component.literal("read-only"); }
        public boolean canSaveDefault() { return saveAllowed && writable; }
        public Component defaultDisabledReason() { return Component.literal("cannot persist"); }
        public RuleEditResult submit(String value) { writes++; this.value = value; return RuleEditResult.applied(); }
        public RuleEditResult saveDefault(String value) { saved = value; return submit(value); }
    }
}

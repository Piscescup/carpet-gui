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

package io.github.piscescup.fabricmc.carpetgui.gui.model;

import io.github.piscescup.fabricmc.carpetgui.api.EditableRuleView;
import io.github.piscescup.fabricmc.carpetgui.api.PersistentRuleEditor;
import io.github.piscescup.fabricmc.carpetgui.api.RuleView;
import io.github.piscescup.fabricmc.carpetgui.store.WorkspaceSession;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Function;

/** Immutable review snapshot shared by modern and legacy widgets. Never edits a rule. */
public record RuleApplyPlan(int revision, Map<String, String> targets, List<Entry> entries, WorkspaceSession.Token session) {
    public RuleApplyPlan {
        targets = Collections.unmodifiableMap(new LinkedHashMap<>(targets));
        entries = List.copyOf(entries);
    }

    public static RuleApplyPlan capture(int revision, Map<String, String> targets, Function<String, RuleView> rules) {
        var entries = targets.entrySet().stream().map(target -> {
            RuleView rule = rules.apply(target.getKey());
            return new Entry(target.getKey(), rule == null ? Component.literal(target.getKey()) : rule.label(),
                rule == null ? null : rule.value(), rule == null ? null : rule.configuredValue().orElse(null),
                rule != null && rule.explicitlyConfigured(), target.getValue(), problem(rule, target.getValue()));
        }).toList();
        return new RuleApplyPlan(revision, targets, entries, WorkspaceSession.current());
    }

    static Component problem(RuleView rule, String value) {
        if (!WorkspaceSession.current().context().connected()) return Component.translatable("carpet-gui.workspace.context_offline");
        if (!(rule instanceof EditableRuleView editable)) return Component.translatable("carpet-gui.workspace.preview_missing");
        if (!editable.editor().editable()) return editable.editor().disabledReason();
        if (!(editable.editor() instanceof PersistentRuleEditor persistent)) return Component.translatable("carpet-gui.workspace.preview_not_persistent");
        if (!persistent.canSaveDefault()) return persistent.defaultDisabledReason();
        if (editable.editor().strict() && !editable.editor().suggestions().contains(value)) return Component.translatable("carpet-gui.edit.invalid_value");
        return null;
    }

    public boolean matches(int currentRevision, Map<String, String> currentHead) {
        return session.currentSession() && revision == currentRevision && targets.equals(currentHead);
    }

    public record Entry(String key, Component label, String current, String configured, boolean explicit,
                        String target, Component problem) {
        public boolean applicable() { return problem == null; }
        public boolean unchanged() { return Objects.equals(current, target); }
        public boolean stillCurrent(RuleView rule) {
            return rule != null && Objects.equals(current, rule.value())
                && Objects.equals(configured, rule.configuredValue().orElse(null)) && explicit == rule.explicitlyConfigured();
        }
    }
}

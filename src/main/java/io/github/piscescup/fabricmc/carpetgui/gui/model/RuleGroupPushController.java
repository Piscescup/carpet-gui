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
import io.github.piscescup.fabricmc.carpetgui.api.RuleView;
import io.github.piscescup.fabricmc.carpetgui.store.RuleGroupStore;
import io.github.piscescup.fabricmc.carpetgui.store.WorkspaceSession;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.Set;
import java.util.LinkedHashSet;

/** Shared by both UI generations; the client tick owns the lifetime, not a row or screen. */
public final class RuleGroupPushController {
    private static AcknowledgedRuleBatch active;
    private static WorkspaceSession.Token activeSession;

    private RuleGroupPushController() {}

    public static boolean busy() { return active != null; }

    public static boolean start(RuleGroupStore.Group group, Function<String, RuleView> rules,
                                Consumer<AcknowledgedRuleBatch.Result> completed) {
        var plan = RuleApplyPlan.capture(group.revision(), group.head(), rules);
        return start(group, plan, plan.targets().keySet(), rules, completed);
    }

    public static boolean start(RuleGroupStore.Group group, RuleApplyPlan plan, Set<String> selected,
                                Function<String, RuleView> rules, Consumer<AcknowledgedRuleBatch.Result> completed) {
        if (busy()) return false;
        var session = plan.session();
        if (!session.canApply()) {
            reject(Component.translatable(session.currentSession() ? "carpet-gui.workspace.context_offline"
                : "carpet-gui.workspace.preview_stale"), selected.size(), completed, session);
            return true;
        }
        if (!plan.matches(group.revision(), group.head())) {
            reject(Component.translatable("carpet-gui.workspace.preview_stale"), selected.size(), completed, session);
            return true;
        }
        Set<String> keys = new LinkedHashSet<>(selected);
        if (!plan.targets().keySet().containsAll(keys) || keys.isEmpty() && !plan.targets().isEmpty()) return false;
        int revision = plan.revision();
        var operations = new ArrayList<AcknowledgedRuleBatch.Operation>();
        for (var entry : plan.entries()) {
            if (!keys.contains(entry.key())) continue;
            RuleView rule = rules.apply(entry.key());
            Component issue = RuleApplyPlan.problem(rule, entry.target());
            if (!entry.applicable() || issue != null || !entry.stillCurrent(rule)) {
                reject(issue != null ? issue : entry.problem() != null ? entry.problem()
                    : Component.translatable("carpet-gui.workspace.preview_stale"), keys.size(), completed, session);
                return true;
            }
        }
        for (var entry : plan.entries()) {
            if (!keys.contains(entry.key())) continue;
            operations.add(callback -> {
                if (!session.canApply()) return RuleEditResult.rejected(Component.translatable("carpet-gui.workspace.preview_stale"));
                RuleView rule = rules.apply(entry.key());
                if (!entry.stillCurrent(rule)) return RuleEditResult.rejected(Component.translatable("carpet-gui.workspace.preview_stale"));
                Component issue = RuleApplyPlan.problem(rule, entry.target());
                if (issue != null) return RuleEditResult.rejected(issue);
                if (!(rule instanceof EditableRuleView editable)) {
                    return RuleEditResult.rejected(Component.translatable("carpet-gui.workspace.push_missing_rule", entry.key()));
                }
                if (!(editable.editor() instanceof PersistentRuleEditor persistent)) {
                    return RuleEditResult.rejected(Component.translatable("carpet-gui.workspace.push_not_persistent", rule.label()));
                }
                if (!persistent.canSaveDefault()) return RuleEditResult.rejected(persistent.defaultDisabledReason());
                return persistent.saveDefault(entry.target(), reply -> {
                    if (session.canApply()) callback.accept(reply);
                });
            });
        }
        activeSession = session;
        active = new AcknowledgedRuleBatch(operations, result -> {
            active = null;
            if (!session.currentSession()) return;
            if (result.success() && keys.equals(plan.targets().keySet())) RuleGroupStore.markPushed(group, revision);
            completed.accept(result);
        });
        return true;
    }

    private static void reject(Component reason, int total, Consumer<AcknowledgedRuleBatch.Result> completed, WorkspaceSession.Token session) {
        activeSession = session;
        active = new AcknowledgedRuleBatch(java.util.List.of(callback -> RuleEditResult.rejected(reason)), result -> {
            active = null;
            if (!session.currentSession()) return;
            completed.accept(new AcknowledgedRuleBatch.Result(0, total, reason));
        });
    }

    public static void tick() {
        if (active != null && !activeSession.currentSession()) {
            active.cancel(Component.translatable("carpet-gui.workspace.preview_stale"));
            return;
        }
        if (active != null) active.tick(System.nanoTime());
    }

    public static void disconnect() {
        if (active != null) active.cancel(Component.translatable("carpet-gui.network.disconnected"));
    }
}

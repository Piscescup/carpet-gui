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

import com.google.gson.Gson;
import io.github.piscescup.fabricmc.carpetgui.store.RuleGroupStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import io.github.piscescup.fabricmc.carpetgui.store.WorkspaceContext;
import io.github.piscescup.fabricmc.carpetgui.store.WorkspaceSession;
import java.util.function.Consumer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RuleGroupPreviewPushTest {
    @BeforeEach void connect() { WorkspaceSession.begin(WorkspaceContext.server("test.invalid", 25565)); }
    @AfterEach void clear() {
        RuleGroupPushController.disconnect();
        WorkspaceSession.begin(WorkspaceContext.offline());
    }
    private RuleGroupStore.Group group() {
        return new Gson().fromJson("{\"id\":\"test\",\"revision\":1,\"head\":{\"a\":\"true\",\"b\":\"true\"}}", RuleGroupStore.Group.class);
    }

    @Test void partialApplicationDoesNotMarkWholeCommitPushed() {
        var group = group();
        var a = new RuleApplyPlanTest.FakeRule();
        var b = new RuleApplyPlanTest.FakeRule();
        var rules = Map.of("a", a, "b", b);
        var plan = RuleApplyPlan.capture(1, group.head(), rules::get);
        var result = new AtomicReference<AcknowledgedRuleBatch.Result>();
        assertTrue(RuleGroupPushController.start(group, plan, Set.of("a"), rules::get, result::set));
        RuleGroupPushController.tick();
        assertTrue(result.get().success());
        assertEquals(1, a.writes);
        assertEquals(0, b.writes);
        assertEquals(1, group.commitsAhead());
    }

    @Test void stalePreviewDoesNotWriteAnyRule() {
        var group = group();
        var a = new RuleApplyPlanTest.FakeRule();
        var plan = RuleApplyPlan.capture(1, group.head(), key -> a);
        a.value = "true";
        var result = new AtomicReference<AcknowledgedRuleBatch.Result>();
        RuleGroupPushController.start(group, plan, Set.of("a"), key -> a, result::set);
        RuleGroupPushController.tick();
        assertFalse(result.get().success());
        assertEquals(0, a.writes);
    }

    @Test void changedPermissionsArePreflightedBeforeAnyWrite() {
        var group = group();
        var a = new RuleApplyPlanTest.FakeRule();
        var b = new RuleApplyPlanTest.FakeRule();
        var rules = Map.of("a", a, "b", b);
        var plan = RuleApplyPlan.capture(1, group.head(), rules::get);
        b.writable = false;
        var result = new AtomicReference<AcknowledgedRuleBatch.Result>();
        RuleGroupPushController.start(group, plan, Set.of("a", "b"), rules::get, result::set);
        RuleGroupPushController.tick();
        assertFalse(result.get().success());
        assertEquals(0, a.writes);
        assertEquals(0, b.writes);
    }

    @Test void editingPermissionIsRecheckedWhenDispatching() {
        var group = group();
        var rule = new RuleApplyPlanTest.FakeRule() {
            @Override public boolean canSaveDefault() { return true; }
        };
        var plan = RuleApplyPlan.capture(1, group.head(), key -> rule);
        var result = new AtomicReference<AcknowledgedRuleBatch.Result>();
        assertTrue(RuleGroupPushController.start(group, plan, Set.of("a"), key -> rule, result::set));
        rule.writable = false;
        RuleGroupPushController.tick();
        assertFalse(result.get().success());
        assertEquals(0, rule.writes);
    }

    @Test void environmentChangeBeforeDispatchCancelsWithoutWritingOrCallingOldUi() {
        var group = group();
        var rule = new RuleApplyPlanTest.FakeRule();
        var plan = RuleApplyPlan.capture(1, group.head(), key -> rule);
        var result = new AtomicReference<AcknowledgedRuleBatch.Result>();
        RuleGroupPushController.start(group, plan, Set.of("a"), key -> rule, result::set);
        WorkspaceSession.begin(WorkspaceContext.server("other.invalid", 25565));
        RuleGroupPushController.tick();
        assertEquals(0, rule.writes);
        assertNull(result.get());
        assertFalse(RuleGroupPushController.busy());
    }

    @Test void lateAcknowledgementCannotFinishOldSessionInNewEnvironment() {
        var group = group();
        var reply = new AtomicReference<Consumer<RuleEditResult>>();
        var rule = new RuleApplyPlanTest.FakeRule() {
            @Override public RuleEditResult saveDefault(String value, Consumer<RuleEditResult> callback) {
                writes++;
                reply.set(callback);
                return RuleEditResult.queuedRequest();
            }
        };
        var plan = RuleApplyPlan.capture(1, group.head(), key -> rule);
        var result = new AtomicReference<AcknowledgedRuleBatch.Result>();
        RuleGroupPushController.start(group, plan, Set.of("a"), key -> rule, result::set);
        RuleGroupPushController.tick();
        assertEquals(1, rule.writes);
        WorkspaceSession.begin(WorkspaceContext.server("other.invalid", 25565));
        reply.get().accept(RuleEditResult.applied());
        RuleGroupPushController.tick();
        assertNull(result.get());
        assertEquals(0, group.pushedRevision());
        assertFalse(RuleGroupPushController.busy());
    }
}

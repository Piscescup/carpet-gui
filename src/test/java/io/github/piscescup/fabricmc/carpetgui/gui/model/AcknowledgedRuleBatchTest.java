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

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class AcknowledgedRuleBatchTest {
    @Test
    void queuedDoesNotCompleteUntilAcknowledged() {
        var callback = new AtomicReference<Consumer<RuleEditResult>>();
        var result = new AtomicReference<AcknowledgedRuleBatch.Result>();
        var batch = new AcknowledgedRuleBatch(List.of(done -> {
            callback.set(done);
            return RuleEditResult.queuedRequest();
        }), result::set);
        batch.tick(0);
        assertNull(result.get());
        callback.get().accept(RuleEditResult.applied());
        assertTrue(result.get().success());
    }

    @Test
    void serverRejectionStopsBatchWithoutCountingQueuedRequest() {
        var callback = new AtomicReference<Consumer<RuleEditResult>>();
        var result = new AtomicReference<AcknowledgedRuleBatch.Result>();
        var batch = new AcknowledgedRuleBatch(List.of(done -> {
            callback.set(done);
            return RuleEditResult.queuedRequest();
        }, done -> fail("Must stop after rejection")), result::set);
        batch.tick(0);
        callback.get().accept(RuleEditResult.rejected(Component.literal("permission")));
        batch.tick(1_000_000_000L);
        assertFalse(result.get().success());
        assertEquals(0, result.get().successful());
    }

    @Test
    void largeBatchIsPacedInsteadOfBurstingPastServerBudget() {
        var sends = new ArrayList<Long>();
        var result = new AtomicReference<AcknowledgedRuleBatch.Result>();
        long[] now = {0};
        var operations = new ArrayList<AcknowledgedRuleBatch.Operation>();
        for (int i = 0; i < 45; i++) operations.add(done -> {
            sends.add(now[0]);
            return RuleEditResult.applied();
        });
        var batch = new AcknowledgedRuleBatch(operations, result::set);
        for (; result.get() == null; now[0] += 50_000_000L) batch.tick(now[0]);
        assertEquals(45, result.get().successful());
        for (int i = 1; i < sends.size(); i++) assertTrue(sends.get(i) - sends.get(i - 1) >= 150_000_000L);
    }

    @Test
    void timeoutAndLateCallbackCompleteOnlyOnce() {
        var callback = new AtomicReference<Consumer<RuleEditResult>>();
        var results = new ArrayList<AcknowledgedRuleBatch.Result>();
        var batch = new AcknowledgedRuleBatch(List.of(done -> {
            callback.set(done);
            return RuleEditResult.queuedRequest();
        }), results::add);
        batch.tick(0);
        batch.tick(10_000_000_000L);
        callback.get().accept(RuleEditResult.applied());
        assertEquals(1, results.size());
        assertFalse(results.getFirst().success());
    }

    @Test
    void synchronousCallbackAndReturnCannotDoubleCount() {
        var results = new ArrayList<AcknowledgedRuleBatch.Result>();
        var batch = new AcknowledgedRuleBatch(List.of(done -> {
            done.accept(RuleEditResult.applied());
            done.accept(RuleEditResult.applied());
            return RuleEditResult.applied();
        }), results::add);
        batch.tick(0);
        assertEquals(1, results.size());
        assertEquals(1, results.getFirst().successful());
    }

    @Test
    void duplicateOldAcknowledgementCannotCompleteNextOperation() {
        var callbacks = new ArrayList<Consumer<RuleEditResult>>();
        var result = new AtomicReference<AcknowledgedRuleBatch.Result>();
        AcknowledgedRuleBatch.Operation operation = done -> {
            callbacks.add(done);
            return RuleEditResult.queuedRequest();
        };
        var batch = new AcknowledgedRuleBatch(List.of(operation, operation), result::set);
        batch.tick(0);
        callbacks.getFirst().accept(RuleEditResult.applied());
        batch.tick(150_000_000L);
        callbacks.getFirst().accept(RuleEditResult.applied());
        assertNull(result.get());
        callbacks.get(1).accept(RuleEditResult.applied());
        assertTrue(result.get().success());
    }

    @Test
    void disconnectAndBackendExceptionCannotSucceed() {
        var result = new AtomicReference<AcknowledgedRuleBatch.Result>();
        var batch = new AcknowledgedRuleBatch(List.of(done -> RuleEditResult.queuedRequest()), result::set);
        batch.tick(0);
        batch.cancel(Component.literal("disconnected"));
        assertFalse(result.get().success());
        var exceptionBatch = new AcknowledgedRuleBatch(List.of(done -> { throw new IllegalStateException(); }), result::set);
        exceptionBatch.tick(0);
        assertFalse(result.get().success());
    }

    @Test
    void emptyDeletionOnlySnapshotCompletesWithoutSendingAnything() {
        var result = new AtomicReference<AcknowledgedRuleBatch.Result>();
        new AcknowledgedRuleBatch(List.of(), result::set).tick(-100);
        assertTrue(result.get().success());
    }
}

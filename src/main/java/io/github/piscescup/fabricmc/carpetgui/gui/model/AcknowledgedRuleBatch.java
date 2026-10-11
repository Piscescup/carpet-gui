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

import java.util.List;
import java.util.function.Consumer;

/** Sequential, paced operations whose queued results are never treated as acknowledgements. */
public final class AcknowledgedRuleBatch {
    private static final long INTERVAL = 150_000_000L;
    private static final long TIMEOUT = 10_000_000_000L;
    private final List<Operation> operations;
    private final Consumer<Result> completed;
    private int successful;
    private long nextSend;
    private long deadline;
    private int generation;
    private boolean waiting;
    private boolean finished;

    public AcknowledgedRuleBatch(List<Operation> operations, Consumer<Result> completed) {
        this.operations = List.copyOf(operations);
        this.completed = completed;
    }

    public void tick(long now) {
        if (finished) return;
        if (waiting) {
            if (now - deadline >= 0) cancel(Component.translatable("carpet-gui.network.timeout"));
            return;
        }
        if (successful == operations.size()) {
            finish(null);
            return;
        }
        if (generation != 0 && now - nextSend < 0) return;
        int token = ++generation;
        waiting = true;
        deadline = now + TIMEOUT;
        nextSend = now + INTERVAL;
        try {
            RuleEditResult immediate = operations.get(successful).submit(reply -> acknowledge(token, reply));
            if (!immediate.queued()) acknowledge(token, immediate);
        } catch (RuntimeException failure) {
            cancel(Component.translatable("carpet-gui.workspace.push_exception"));
        }
    }

    private void acknowledge(int token, RuleEditResult reply) {
        if (finished || !waiting || token != generation || reply.queued()) return;
        waiting = false;
        if (!reply.accepted()) finish(reply.message());
        else if (++successful == operations.size()) finish(null);
    }

    public void cancel(Component reason) {
        if (!finished) finish(reason);
    }

    private void finish(Component failure) {
        finished = true;
        waiting = false;
        completed.accept(new Result(successful, operations.size(), failure));
    }

    @FunctionalInterface
    public interface Operation {
        RuleEditResult submit(Consumer<RuleEditResult> completed);
    }

    public record Result(int successful, int total, Component failure) {
        public boolean success() { return failure == null && successful == total; }
    }
}

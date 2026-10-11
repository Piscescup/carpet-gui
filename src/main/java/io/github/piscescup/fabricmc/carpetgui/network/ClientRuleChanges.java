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

package io.github.piscescup.fabricmc.carpetgui.network;

import carpet.api.settings.InvalidRuleValueException;
import io.github.piscescup.fabricmc.carpetgui.References;
import io.github.piscescup.fabricmc.carpetgui.store.CarpetManagerBinding;
import io.github.piscescup.fabricmc.carpetgui.integration.carpet.CarpetRuleSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** Server-authored edit state and per-rule history for the current connection. */
public final class ClientRuleChanges {
    private static final Map<String, RuleChangeEvent> LATEST = new LinkedHashMap<>();
    private static final Map<String, List<RuleChangeEvent>> HISTORY = new LinkedHashMap<>();
    private static final AtomicLong REVISION = new AtomicLong();

    private ClientRuleChanges() {}

    public static void initialize() {
        References.LOGGER.info(
            "Initializing client rule changes of {} ver {}",
            References.MOD_NAME, References.MOD_VERSION
        );
        ClientPlayNetworking.registerGlobalReceiver(
            RuleChangeEvent.TYPE,
            (event, context) -> receive(event)
        );

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LATEST.clear();
            HISTORY.clear();
            REVISION.incrementAndGet();
        });
    }

    public static RuleChangeEvent latest(String stateId) {
        return LATEST.get(stateId);
    }

    /** Newest first, so older changes are rendered lower in a tooltip. */
    public static List<RuleChangeEvent> history(String stateId) {
        List<RuleChangeEvent> history = HISTORY.get(stateId);
        return history == null ? List.of() : history.stream()
            .sorted(Comparator.comparingLong(RuleChangeEvent::changedAt).reversed())
            .toList();
    }

    public static long revision() {
        return REVISION.get();
    }

    public static void clear(String stateId) {
        if (LATEST.remove(stateId) != null) REVISION.incrementAndGet();
    }

    static void applyServerValue(String managerId, String ruleId, String value) {
        if (value.isEmpty()) return;
        for (var binding : CarpetManagerBinding.discover()) {
            if (!binding.manager().identifier().equals(managerId)) continue;
            var rule = binding.manager().getCarpetRule(ruleId);
            if (rule == null) continue;
            try {
                rule.set(null, value);
                CarpetRuleSource.RULE_REVISION.incrementAndGet();
            } catch (InvalidRuleValueException ignored) {}
            return;
        }
    }

    private static void receive(RuleChangeEvent event) {
        if (!event.valid()) return;
        var player = Minecraft.getInstance().player;
        boolean local = player != null && event.actorId().equals(player.getUUID().toString());
        if (!local) {
            applyServerValue(event.managerId(), event.ruleId(), event.newValue());
            LATEST.put(event.stateId(), event);
        }
        HISTORY.computeIfAbsent(event.stateId(), ignored -> new java.util.ArrayList<>()).add(event);
        REVISION.incrementAndGet();
    }
}

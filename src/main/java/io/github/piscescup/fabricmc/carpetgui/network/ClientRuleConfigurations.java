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

package io.github.piscescup.fabricmc.carpetgui.network;

import io.github.piscescup.fabricmc.carpetgui.References;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/** Server-authored effective configuration values for the current connection. */
public final class ClientRuleConfigurations {
    private static final Map<String, Configuration> VALUES = new LinkedHashMap<>();
    private static final AtomicLong REVISION = new AtomicLong();
    private static boolean ready;

    private ClientRuleConfigurations() {}

    public static void initialize() {
        References.LOGGER.info(
            "Initializing client rule configuration sync of {} ver {}",
            References.MOD_NAME, References.MOD_VERSION
        );
        ClientPlayNetworking.registerGlobalReceiver(
            RuleConfigurationSnapshot.TYPE,
            (snapshot, context) -> receive(snapshot)
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
    }

    public static Optional<String> value(String stateId) {
        return Optional.ofNullable(VALUES.get(stateId)).map(Configuration::value);
    }

    public static boolean explicitlyConfigured(String stateId) {
        Configuration configuration = VALUES.get(stateId);
        return configuration != null && configuration.explicitlyConfigured();
    }

    public static boolean ready() {
        return ready;
    }

    public static long revision() {
        return REVISION.get();
    }

    private static void receive(RuleConfigurationSnapshot snapshot) {
        if (!snapshot.valid()) return;
        if (snapshot.replace()) VALUES.clear();
        for (RuleConfigurationSnapshot.Entry entry : snapshot.entries()) {
            VALUES.put(entry.stateId(), new Configuration(entry.value(), entry.explicitlyConfigured()));
        }
        if (snapshot.replace()) ready = true;
        REVISION.incrementAndGet();
    }

    private static void clear() {
        VALUES.clear();
        ready = false;
        REVISION.incrementAndGet();
    }

    private record Configuration(String value, boolean explicitlyConfigured) {}
}

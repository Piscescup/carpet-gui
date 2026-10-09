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

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.RuleHelper;
import carpet.api.settings.SettingsManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Audits player-originated Carpet changes and broadcasts a server-authored old/new transition. */
final class ServerRuleChanges {
    private static final Map<String, String> VALUES = new LinkedHashMap<>();
    private static final Map<String, RuleConfigurationSnapshot.Entry> CONFIGURED_VALUES = new LinkedHashMap<>();
    private static final Map<String, String> CONFIGURED_FILE_VALUES = new LinkedHashMap<>();

    private ServerRuleChanges() {}

    static void initialize() {
        SettingsManager.registerGlobalRuleObserver(ServerRuleChanges::changed);
        ServerLifecycleEvents.SERVER_STARTED.register(ServerRuleChanges::snapshot);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            VALUES.clear();
            CONFIGURED_VALUES.clear();
            CONFIGURED_FILE_VALUES.clear();
        });
    }

    private static void snapshot(MinecraftServer server) {
        VALUES.clear();
        CONFIGURED_VALUES.clear();
        CONFIGURED_FILE_VALUES.clear();
        for (SettingsManager manager : managers()) {
            Map<String, String> configured = readConfiguration(server, manager);
            if (configured == null) configured = Map.of();
            for (CarpetRule<?> rule : manager.getCarpetRules()) {
                String value = RuleHelper.toRuleString(rule.value());
                VALUES.put(stateId(rule), value);
                if (value.length() <= 4096) {
                    var entry = entry(rule, value, configured.containsKey(rule.name()));
                    CONFIGURED_VALUES.put(entry.stateId(), entry);
                    if (entry.explicitlyConfigured()) CONFIGURED_FILE_VALUES.put(entry.stateId(), configured.get(rule.name()));
                }
            }
        }
    }

    static RuleConfigurationSnapshot configurationSnapshot(MinecraftServer server) {
        // Include rules registered after SERVER_STARTED without disturbing an existing configuration baseline.
        for (SettingsManager manager : managers()) {
            Map<String, String> configured = readConfiguration(server, manager);
            if (configured == null) configured = Map.of();
            for (CarpetRule<?> rule : manager.getCarpetRules()) {
                String stateId = stateId(rule);
                if (CONFIGURED_VALUES.containsKey(stateId)) continue;
                String value = RuleHelper.toRuleString(rule.value());
                if (value.length() <= 4096) {
                    var entry = entry(rule, value, configured.containsKey(rule.name()));
                    CONFIGURED_VALUES.put(stateId, entry);
                    if (entry.explicitlyConfigured()) CONFIGURED_FILE_VALUES.put(stateId, configured.get(rule.name()));
                }
            }
        }
        return new RuleConfigurationSnapshot(
            RuleConfigurationSnapshot.VERSION, true, List.copyOf(CONFIGURED_VALUES.values())
        );
    }

    static void configurationSaved(MinecraftServer server, CarpetRule<?> rule) {
        String value = RuleHelper.toRuleString(rule.value());
        if (value.length() > 4096) return;
        var entry = entry(rule, value, true);
        CONFIGURED_VALUES.put(entry.stateId(), entry);
        Map<String, String> configured = readConfiguration(server, rule.settingsManager());
        if (configured != null && configured.containsKey(rule.name())) {
            CONFIGURED_FILE_VALUES.put(entry.stateId(), configured.get(rule.name()));
        }
        broadcast(server, List.of(entry));
    }

    private static void refreshConfiguration(MinecraftServer server, SettingsManager manager) {
        Map<String, String> configured = readConfiguration(server, manager);
        if (configured == null) return;
        var updates = new java.util.ArrayList<RuleConfigurationSnapshot.Entry>();
        for (CarpetRule<?> rule : manager.getCarpetRules()) {
            String stateId = stateId(rule);
            var previous = CONFIGURED_VALUES.get(stateId);
            boolean explicit = configured.containsKey(rule.name());
            if (!explicit && previous != null && !previous.explicitlyConfigured()) continue;
            String fileValue = configured.get(rule.name());
            String value = explicit && previous != null && previous.explicitlyConfigured()
                           && Objects.equals(fileValue, CONFIGURED_FILE_VALUES.get(stateId))
                ? previous.value() : RuleHelper.toRuleString(rule.value());
            if (value.length() > 4096) continue;
            var current = entry(rule, value, explicit);
            if (explicit) CONFIGURED_FILE_VALUES.put(stateId, fileValue);
            else CONFIGURED_FILE_VALUES.remove(stateId);
            if (current.equals(previous)) continue;
            CONFIGURED_VALUES.put(stateId, current);
            updates.add(current);
        }
        if (!updates.isEmpty()) broadcast(server, updates);
    }

    private static void broadcast(MinecraftServer server, List<RuleConfigurationSnapshot.Entry> entries) {
        var update = new RuleConfigurationSnapshot(RuleConfigurationSnapshot.VERSION, false, entries);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (ServerPlayNetworking.canSend(player, RuleConfigurationSnapshot.TYPE)) {
                ServerPlayNetworking.send(player, update);
            }
        }
    }

    private static void changed(CommandSourceStack source, CarpetRule<?> rule, String suppliedValue) {
        if (source != null) {
            // setDefault writes the file after notifying observers. Queue the read so command-driven
            // additions and removals are visible even when the effective value did not change.
            source.getServer().execute(() -> refreshConfiguration(source.getServer(), rule.settingsManager()));
        }
        String newValue = RuleHelper.toRuleString(rule.value());
        String oldValue = VALUES.put(stateId(rule), newValue);
        if (oldValue == null || oldValue.equals(newValue) || source == null
            || !(source.getEntity() instanceof ServerPlayer actor)
            || oldValue.length() > 4096 || newValue.length() > 4096) {
            return;
        }
        var event = new RuleChangeEvent(
            RuleChangeEvent.VERSION, rule.settingsManager().identifier(), rule.name(), RuleEditRequest.SET_VALUE,
            actor.getUUID().toString(), actor.getName().getString(), oldValue, newValue, System.currentTimeMillis()
        );
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            if (ServerPlayNetworking.canSend(player, RuleChangeEvent.TYPE)) {
                ServerPlayNetworking.send(player, event);
            }
        }
    }

    private static List<SettingsManager> managers() {
        var result = new java.util.ArrayList<SettingsManager>();
        if (CarpetServer.settingsManager != null) result.add(CarpetServer.settingsManager);
        for (var extension : List.copyOf(CarpetServer.extensions)) {
            SettingsManager manager = extension.extensionSettingsManager();
            if (manager != null && !result.contains(manager)) result.add(manager);
        }
        return result;
    }

    private static String stateId(CarpetRule<?> rule) {
        return rule.settingsManager().identifier() + ":" + rule.name();
    }

    private static Map<String, String> readConfiguration(MinecraftServer server, SettingsManager manager) {
        var path = server.getWorldPath(LevelResource.ROOT).resolve(manager.identifier() + ".conf");
        if (Files.notExists(path)) return Map.of();
        try {
            Map<String, String> result = new LinkedHashMap<>();
            for (String line : Files.readAllLines(path)) {
                String[] fields = line.replaceAll("[\\r\\n]", "").split("\\s+", 2);
                if (fields.length <= 1 || fields[1].startsWith("#") || manager.getCarpetRule(fields[0]) == null) continue;
                result.put(fields[0], fields[1]);
            }
            return result;
        } catch (IOException exception) {
            io.github.piscescup.fabricmc.carpetgui.References.LOGGER.warn(
                "Could not read Carpet configuration {}", path, exception
            );
            return null;
        }
    }

    private static RuleConfigurationSnapshot.Entry entry(CarpetRule<?> rule, String value, boolean explicitlyConfigured) {
        return new RuleConfigurationSnapshot.Entry(
            rule.settingsManager().identifier(), rule.name(), value, explicitlyConfigured
        );
    }
}

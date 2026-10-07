package io.github.piscescup.fabricmc.carpetgui.network;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.RuleHelper;
import carpet.api.settings.SettingsManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Audits player-originated Carpet changes and broadcasts a server-authored old/new transition. */
final class ServerRuleChanges {
    private static final Map<String, String> VALUES = new LinkedHashMap<>();

    private ServerRuleChanges() {}

    static void initialize() {
        SettingsManager.registerGlobalRuleObserver(ServerRuleChanges::changed);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> snapshot());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> VALUES.clear());
    }

    private static void snapshot() {
        VALUES.clear();
        for (SettingsManager manager : managers()) {
            for (CarpetRule<?> rule : manager.getCarpetRules()) {
                VALUES.put(stateId(rule), RuleHelper.toRuleString(rule.value()));
            }
        }
    }

    private static void changed(CommandSourceStack source, CarpetRule<?> rule, String suppliedValue) {
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
}

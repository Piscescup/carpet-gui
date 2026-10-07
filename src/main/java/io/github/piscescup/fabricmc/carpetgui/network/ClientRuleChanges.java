package io.github.piscescup.fabricmc.carpetgui.network;

import carpet.api.settings.InvalidRuleValueException;
import io.github.piscescup.fabricmc.carpetgui.integration.carpet.CarpetManagerBinding;
import io.github.piscescup.fabricmc.carpetgui.integration.carpet.CarpetRuleSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** Latest server-authored edit per rule for the current connection. */
public final class ClientRuleChanges {
    private static final Map<String, RuleChangeEvent> LATEST = new LinkedHashMap<>();
    private static final AtomicLong REVISION = new AtomicLong();

    private ClientRuleChanges() {}

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(RuleChangeEvent.TYPE, (event, context) -> receive(event));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LATEST.clear();
            REVISION.incrementAndGet();
        });
    }

    public static RuleChangeEvent latest(String stateId) {
        return LATEST.get(stateId);
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
            } catch (InvalidRuleValueException ignored) {
            }
            return;
        }
    }

    private static void receive(RuleChangeEvent event) {
        if (!event.valid()) return;
        var player = Minecraft.getInstance().player;
        if (player != null && event.actorId().equals(player.getUUID().toString())) return;
        applyServerValue(event.managerId(), event.ruleId(), event.newValue());
        LATEST.put(event.stateId(), event);
        REVISION.incrementAndGet();
    }
}

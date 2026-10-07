package io.github.piscescup.fabricmc.carpetgui.network;

import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleEditResult;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;

/** Client-thread request tracking. Timeout is uncertain, so it never retries through the command fallback. */
public final class ClientRuleNetworking {
    private static final LinkedHashMap<Long, Pending> PENDING = new LinkedHashMap<>();
    private static long nextId;
    private static long completed;
    private static RuleEditResponse lastResponse;
    private ClientRuleNetworking() {}
    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(RuleEditResponse.TYPE, (response, context) -> receive(response));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
    }
    public static boolean supported() { return ClientPlayNetworking.canSend(RuleEditRequest.TYPE); }
    public static RuleEditResult send(String manager, String rule, String value, boolean saveDefault, Consumer<RuleEditResult> callback) {
        if (!supported()) return RuleEditResult.rejected(Component.translatable("carpet-gui.network.unavailable"));
        if (PENDING.size() >= 32) return RuleEditResult.rejected(Component.translatable("carpet-gui.network.busy"));
        var request = new RuleEditRequest(RuleEditRequest.VERSION, ++nextId, manager, rule,
            saveDefault ? RuleEditRequest.SAVE_DEFAULT : RuleEditRequest.SET_VALUE, value);
        if (!request.valid()) return RuleEditResult.rejected(Component.translatable("carpet-gui.network.bad_request"));
        PENDING.put(request.requestId(), new Pending(request, System.nanoTime(), callback));
        ClientPlayNetworking.send(request);
        return RuleEditResult.queuedRequest(Component.translatable("carpet-gui.network.pending"));
    }
    private static void receive(RuleEditResponse response) {
        var pending = PENDING.get(response.requestId());
        if (pending == null || !response.matches(pending.request())) return;
        PENDING.remove(response.requestId());
        completed++;
        lastResponse = response;
        if (!response.value().isEmpty()) {
            // Apply only a matched server acknowledgement, just as Carpet's own rule synchronization does.
            ClientRuleChanges.applyServerValue(response.managerId(), response.ruleId(), response.value());
        }
        if (response.success()) ClientRuleChanges.clear(response.managerId() + ":" + response.ruleId());
        pending.callback().accept(new RuleEditResult(response.success(), false, Component.translatable(response.messageKey())));
    }
    public static void tick() {
        long now = System.nanoTime();
        var expired = PENDING.values().stream().filter(pending -> now - pending.started() > 10_000_000_000L).toList();
        for (var pending : expired) {
            PENDING.remove(pending.request().requestId());
            pending.callback().accept(RuleEditResult.rejected(Component.translatable("carpet-gui.network.timeout")));
        }
    }
    private static void clear() {
        var abandoned = List.copyOf(PENDING.values());
        PENDING.clear();
        lastResponse = null;
        for (var pending : abandoned)
            pending.callback().accept(RuleEditResult.rejected(Component.translatable("carpet-gui.network.disconnected")));
    }
    public static int pendingCount() { return PENDING.size(); }
    public static long completedCount() { return completed; }
    public static RuleEditResponse lastResponse() { return lastResponse; }
    private record Pending(RuleEditRequest request, long started, Consumer<RuleEditResult> callback) {}
}

package io.github.piscescup.fabricmc.carpetgui.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.WeakHashMap;

import static io.github.piscescup.fabricmc.carpetgui.References.*;

/**
 * Common entrypoint: safe on dedicated servers, with per-connection replay/rate protection.
 */
public final class RuleNetworking {
    private static final WeakHashMap<ServerPlayer, Budget> BUDGETS = new WeakHashMap<>();

    private RuleNetworking() {
    }

    public static void initialize() {
        LOGGER.info("Initialize Rule Networking of {} ver {}", MOD_NAME, MOD_VERSION);

        PayloadTypeRegistry.serverboundPlay()
            .register(RuleEditRequest.TYPE, RuleEditRequest.CODEC);
        PayloadTypeRegistry.clientboundPlay()
            .register(RuleEditResponse.TYPE, RuleEditResponse.CODEC);
        PayloadTypeRegistry.clientboundPlay()
            .register(RuleChangeEvent.TYPE, RuleChangeEvent.CODEC);
        ServerRuleChanges.initialize();
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> BUDGETS.remove(handler.player));
        ServerPlayNetworking.registerGlobalReceiver(
            RuleEditRequest.TYPE, (request, context) -> {
                // Explicit scheduling also keeps all mutable per-connection state on the server thread.
                context.server()
                    .execute(() -> {
                        if (context.player()
                                .hasDisconnected() || !ServerPlayNetworking.canSend(context.player(), RuleEditResponse.TYPE)) {
                            return;
                        }
                        var budget = BUDGETS.computeIfAbsent(context.player(), ignored -> new Budget());
                        int tick = context.server()
                            .getTickCount();
                        if (tick - budget.windowStart >= 20) {
                            budget.windowStart = tick;
                            budget.count = 0;
                        }
                        RuleEditResponse response;
                        if (++budget.count > 20) {
                            response = RuleEditResponse.result(request, false, "", "carpet-gui.network.rate_limit");
                        } else if (request.requestId() <= budget.lastId) {
                            response = RuleEditResponse.result(request, false, "", "carpet-gui.network.bad_request");
                        } else {
                            budget.lastId = request.requestId();
                            response = ServerRuleEdits.execute(
                                request,
                                context.player()
                                    .createCommandSourceStack()
                            );
                        }
                        ServerPlayNetworking.send(context.player(), response);
                    });
            }
        );
    }

    private static final class Budget {
        private long lastId;
        private int windowStart;
        private int count;
    }
}

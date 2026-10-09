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

        //#if MC >= 260000
        PayloadTypeRegistry.serverboundPlay()
        //#else
        //$$ PayloadTypeRegistry.playC2S()
        //#endif
            .register(RuleEditRequest.TYPE, RuleEditRequest.CODEC);
        //#if MC >= 260000
        PayloadTypeRegistry.clientboundPlay()
        //#else
        //$$ PayloadTypeRegistry.playS2C()
        //#endif
            .register(RuleEditResponse.TYPE, RuleEditResponse.CODEC);
        //#if MC >= 260000
        PayloadTypeRegistry.clientboundPlay()
        //#else
        //$$ PayloadTypeRegistry.playS2C()
        //#endif
            .register(RuleChangeEvent.TYPE, RuleChangeEvent.CODEC);
        //#if MC >= 260000
        PayloadTypeRegistry.clientboundPlay()
        //#else
        //$$ PayloadTypeRegistry.playS2C()
        //#endif
            .register(RuleConfigurationSnapshot.TYPE, RuleConfigurationSnapshot.CODEC);
        ServerRuleChanges.initialize();
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (ServerPlayNetworking.canSend(handler.player, RuleConfigurationSnapshot.TYPE)) {
                ServerPlayNetworking.send(handler.player, ServerRuleChanges.configurationSnapshot(server));
            }
        });
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

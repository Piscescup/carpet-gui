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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

/**
 * A result belongs to exactly one request; value is the server's resulting value, never an optimistic guess.
 */
public record RuleEditResponse(
    int version,
    long requestId,
    String managerId,
    String ruleId,
    int operation,
    boolean success,
    String value,
    String messageKey
)
    implements CustomPacketPayload
{
    public static final Type<RuleEditResponse> TYPE = new Type<>(References.fromPath("rule_result_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RuleEditResponse> CODEC = new StreamCodec<>() {
        @NonNull
        @Override
        public RuleEditResponse decode(RegistryFriendlyByteBuf buffer) {
            return new RuleEditResponse(
                buffer.readVarInt(), buffer.readVarLong(), buffer.readUtf(128), buffer.readUtf(128),
                buffer.readVarInt(), buffer.readBoolean(), buffer.readUtf(4096), buffer.readUtf(128)
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RuleEditResponse response) {
            buffer.writeVarInt(response.version());
            buffer.writeVarLong(response.requestId());
            buffer.writeUtf(response.managerId(), 128);
            buffer.writeUtf(response.ruleId(), 128);
            buffer.writeVarInt(response.operation());
            buffer.writeBoolean(response.success());
            buffer.writeUtf(response.value(), 4096);
            buffer.writeUtf(response.messageKey(), 128);
        }
    };

    public static RuleEditResponse result(RuleEditRequest request, boolean success, String value, String key) {
        return new RuleEditResponse(
            RuleEditRequest.VERSION, request.requestId(), request.managerId(), request.ruleId(),
            request.operation(), success, value.length() <= 4096 ? value : "", key
        );
    }

    public boolean matches(RuleEditRequest request) {
        return version == RuleEditRequest.VERSION && requestId == request.requestId() && managerId.equals(request.managerId())
               && ruleId.equals(request.ruleId()) && operation == request.operation();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

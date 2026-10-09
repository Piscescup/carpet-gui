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
import org.jspecify.annotations.NonNull;

/**
 * Versioned, bounded request. No player identity, permission claim or arbitrary command is accepted.
 */
public record RuleEditRequest(
    int version,
    long requestId,
    String managerId,
    String ruleId,
    int operation,
    String value
)
    implements CustomPacketPayload
{
    public static final int VERSION = 1;
    public static final int SET_VALUE = 0;
    public static final int SAVE_DEFAULT = 1;
    public static final Type<RuleEditRequest> TYPE = new Type<>(References.fromPath("rule_edit_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RuleEditRequest> CODEC = new StreamCodec<>() {
        @NonNull
        @Override
        public RuleEditRequest decode(RegistryFriendlyByteBuf buffer) {
            return new RuleEditRequest(
                buffer.readVarInt(), buffer.readVarLong(), buffer.readUtf(128), buffer.readUtf(128),
                buffer.readVarInt(), buffer.readUtf(256)
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RuleEditRequest request) {
            buffer.writeVarInt(request.version());
            buffer.writeVarLong(request.requestId());
            buffer.writeUtf(request.managerId(), 128);
            buffer.writeUtf(request.ruleId(), 128);
            buffer.writeVarInt(request.operation());
            buffer.writeUtf(request.value(), 256);
        }
    };

    public boolean valid() {
        return version == VERSION && requestId > 0 && managerId.matches("[A-Za-z0-9_.-]{1,128}")
               && ruleId.matches("[A-Za-z0-9_]{1,128}") && (operation == SET_VALUE || operation == SAVE_DEFAULT)
               && !value.isEmpty() && value.length() <= 256 && value.chars()
                   .noneMatch(Character::isISOControl);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

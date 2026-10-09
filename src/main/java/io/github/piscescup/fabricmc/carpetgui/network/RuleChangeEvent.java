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

/** Server-authored event broadcast after a successful GUI rule edit. */
public record RuleChangeEvent(
    int version,
    String managerId,
    String ruleId,
    int operation,
    String actorId,
    String actorName,
    String oldValue,
    String newValue,
    long changedAt
)
    implements CustomPacketPayload
{
    public static final int VERSION = 1;
    public static final Type<RuleChangeEvent> TYPE = new Type<>(References.fromPath("rule_change_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RuleChangeEvent> CODEC = new StreamCodec<>() {
        @NonNull
        @Override
        public RuleChangeEvent decode(RegistryFriendlyByteBuf buffer) {
            return new RuleChangeEvent(
                buffer.readVarInt(), buffer.readUtf(128), buffer.readUtf(128), buffer.readVarInt(),
                buffer.readUtf(64), buffer.readUtf(64), buffer.readUtf(4096), buffer.readUtf(4096),
                buffer.readVarLong()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RuleChangeEvent event) {
            buffer.writeVarInt(event.version());
            buffer.writeUtf(event.managerId(), 128);
            buffer.writeUtf(event.ruleId(), 128);
            buffer.writeVarInt(event.operation());
            buffer.writeUtf(event.actorId(), 64);
            buffer.writeUtf(event.actorName(), 64);
            buffer.writeUtf(event.oldValue(), 4096);
            buffer.writeUtf(event.newValue(), 4096);
            buffer.writeVarLong(event.changedAt());
        }
    };

    public boolean valid() {
        return version == VERSION && managerId.length() <= 128 && ruleId.length() <= 128
               && actorId.length() <= 64 && actorName.length() <= 64
               && oldValue.length() <= 4096 && newValue.length() <= 4096
               && (operation == RuleEditRequest.SET_VALUE || operation == RuleEditRequest.SAVE_DEFAULT);
    }

    public String stateId() {
        return managerId + ":" + ruleId;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

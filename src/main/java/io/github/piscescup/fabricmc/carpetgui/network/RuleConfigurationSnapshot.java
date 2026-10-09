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

import java.util.ArrayList;
import java.util.List;

/** Effective Carpet configuration values and their explicit world-config presence. */
public record RuleConfigurationSnapshot(
    int version,
    boolean replace,
    List<Entry> entries
)
    implements CustomPacketPayload
{
    public static final int VERSION = 2;
    private static final int MAX_ENTRIES = 8192;
    public static final Type<RuleConfigurationSnapshot> TYPE =
        new Type<>(References.fromPath("rule_configuration_v2"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RuleConfigurationSnapshot> CODEC = new StreamCodec<>() {
        @NonNull
        @Override
        public RuleConfigurationSnapshot decode(RegistryFriendlyByteBuf buffer) {
            int version = buffer.readVarInt();
            boolean replace = buffer.readBoolean();
            int size = buffer.readVarInt();
            if (size < 0 || size > MAX_ENTRIES) {
                throw new IllegalArgumentException("Invalid Carpet rule configuration entry count: " + size);
            }
            List<Entry> entries = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                entries.add(new Entry(
                    buffer.readUtf(128), buffer.readUtf(128), buffer.readUtf(4096), buffer.readBoolean()
                ));
            }
            return new RuleConfigurationSnapshot(version, replace, List.copyOf(entries));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RuleConfigurationSnapshot snapshot) {
            buffer.writeVarInt(snapshot.version());
            buffer.writeBoolean(snapshot.replace());
            buffer.writeVarInt(snapshot.entries().size());
            for (Entry entry : snapshot.entries()) {
                buffer.writeUtf(entry.managerId(), 128);
                buffer.writeUtf(entry.ruleId(), 128);
                buffer.writeUtf(entry.value(), 4096);
                buffer.writeBoolean(entry.explicitlyConfigured());
            }
        }
    };

    public RuleConfigurationSnapshot {
        entries = List.copyOf(entries);
    }

    public boolean valid() {
        return version == VERSION && entries.size() <= MAX_ENTRIES && entries.stream().allMatch(Entry::valid);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(String managerId, String ruleId, String value, boolean explicitlyConfigured) {
        public boolean valid() {
            return !managerId.isBlank() && managerId.length() <= 128
                   && !ruleId.isBlank() && ruleId.length() <= 128 && value.length() <= 4096;
        }

        public String stateId() {
            return managerId + ":" + ruleId;
        }
    }
}

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

package io.github.piscescup.fabricmc.carpetgui.store;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.gamerules.GameRule;

import java.util.Map;

/**
 * Server-returned gamerule values, cleared between connections. Never substitutes defaults for unknown values.
 */
public final class VanillaRuleStore {
    private static Map<ResourceKey<GameRule<?>>, String> values = Map.of();
    private static boolean received;
    private static long revision;

    private VanillaRuleStore() {
    }

    public static Map<ResourceKey<GameRule<?>>, String> values() {
        return values;
    }

    public static boolean received() {
        return received;
    }

    public static long revision() {
        return revision;
    }

    public static void accept(Map<ResourceKey<GameRule<?>>, String> snapshot) {
        if (!received || !values.equals(snapshot)) revision++;
        values = Map.copyOf(snapshot);
        received = true;
    }

    public static void clear() {
        values = Map.of();
        received = false;
        revision++;
    }

    public static boolean permitted(Minecraft client) {
        return client.player != null && client.getConnection() != null
               && Commands.LEVEL_GAMEMASTERS.check(client.player.permissions());
    }

    public static void request(Minecraft client) {
        //#if MC >= 260000
        if (permitted(client)) {
            client.getConnection()
                .send(new ServerboundClientCommandPacket(
                    ServerboundClientCommandPacket.Action.REQUEST_GAMERULE_VALUES));
        }
        //#endif
    }
}

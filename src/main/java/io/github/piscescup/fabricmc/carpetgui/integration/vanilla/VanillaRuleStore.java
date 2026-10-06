package io.github.piscescup.fabricmc.carpetgui.integration.vanilla;

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
        if (permitted(client)) {
            client.getConnection()
                .send(new ServerboundClientCommandPacket(
                    ServerboundClientCommandPacket.Action.REQUEST_GAMERULE_VALUES));
        }
    }
}

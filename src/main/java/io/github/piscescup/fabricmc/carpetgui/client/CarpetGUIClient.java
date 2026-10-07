package io.github.piscescup.fabricmc.carpetgui.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.piscescup.fabricmc.carpetgui.References;
import io.github.piscescup.fabricmc.carpetgui.api.CarpetGuiScreens;
import io.github.piscescup.fabricmc.carpetgui.integration.carpet.CarpetRuleSource;
import io.github.piscescup.fabricmc.carpetgui.integration.vanilla.VanillaRuleStore;
import io.github.piscescup.fabricmc.carpetgui.network.ClientRuleNetworking;
import carpet.api.settings.SettingsManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class CarpetGUIClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientRuleNetworking.initialize();
        SettingsManager.registerGlobalRuleObserver((source, rule, value) -> CarpetRuleSource.RULE_REVISION.incrementAndGet());
        ClientPlayConnectionEvents.DISCONNECT.register((connection, client) -> VanillaRuleStore.clear());
        KeyMapping openRules = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.carpet-gui.open_rules", InputConstants.Type.KEYBOARD, InputConstants.KEY_F9,
                KeyMapping.Category.register(References.fromPath("main")))
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientRuleNetworking.tick();
            while (openRules.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(CarpetGuiScreens.create(null));
                }
            }
        });
    }
}

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

package io.github.piscescup.fabricmc.carpetgui.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.piscescup.fabricmc.carpetgui.References;
import io.github.piscescup.fabricmc.carpetgui.api.CarpetGuiScreens;
import io.github.piscescup.fabricmc.carpetgui.integration.carpet.CarpetRuleSource;
//#if MC >= 260000
import io.github.piscescup.fabricmc.carpetgui.integration.vanilla.VanillaRuleStore;
//#endif
import io.github.piscescup.fabricmc.carpetgui.network.ClientRuleNetworking;
import io.github.piscescup.fabricmc.carpetgui.network.ClientRuleChanges;
import io.github.piscescup.fabricmc.carpetgui.network.ClientRuleConfigurations;
import carpet.api.settings.SettingsManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//#if MC >= 260000
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
//#else
//$$ import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
//#endif
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class CarpetGUIClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientRuleNetworking.initialize();
        ClientRuleChanges.initialize();
        ClientRuleConfigurations.initialize();
        SettingsManager.registerGlobalRuleObserver((source, rule, value) -> CarpetRuleSource.RULE_REVISION.incrementAndGet());
        //#if MC >= 260000
        ClientPlayConnectionEvents.DISCONNECT.register((connection, client) -> VanillaRuleStore.clear());
        //#endif
        //#if MC >= 260000
        KeyMapping openRules = KeyMappingHelper.registerKeyMapping(new KeyMapping(
        //#else
        //$$ KeyMapping openRules = KeyBindingHelper.registerKeyBinding(new KeyMapping(
        //#endif
                "key.carpet-gui.open_rules",
                //#if MC >= 260300
                InputConstants.Type.KEYBOARD,
                //#else
                //$$ InputConstants.Type.KEYSYM,
                //#endif
                InputConstants.KEY_F9,
                //#if MC >= 12109
                KeyMapping.Category.register(References.fromPath("main")))
                //#else
                //$$ "key.categories.carpet-gui")
                //#endif
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientRuleNetworking.tick();
            while (openRules.consumeClick()) {
                //#if MC >= 260200
                if (client.gui.screen() == null) {
                    client.gui.setScreen(CarpetGuiScreens.create(null));
                //#else
                //$$ if (client.screen == null) {
                //$$     client.setScreen(CarpetGuiScreens.create(null));
                //#endif
                }
            }
        });
    }
}

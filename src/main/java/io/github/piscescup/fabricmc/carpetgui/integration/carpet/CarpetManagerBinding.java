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

package io.github.piscescup.fabricmc.carpetgui.integration.carpet;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.api.settings.SettingsManager;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/**
 * Manager IDs (command roots) and Fabric mod IDs are intentionally kept distinct.
 * A null mod ID means no API or adapter declared ownership; rule providers can still claim its rules.
 */
public record CarpetManagerBinding(
    SettingsManager manager,
    String modId,
    Component title
) {
    public static List<CarpetManagerBinding> discover() {
        return discover(CarpetTranslationRegistry.discover());
    }

    public static List<CarpetManagerBinding> discover(CarpetTranslationRegistry translations) {
        var bindings = new LinkedHashMap<String, CarpetManagerBinding>();
        if (CarpetServer.settingsManager != null) add(bindings, CarpetServer.settingsManager, translations);
        for (CarpetExtension extension : List.copyOf(CarpetServer.extensions)) {
            SettingsManager manager = extension.extensionSettingsManager();
            if (manager != null) add(bindings, manager, translations);
        }
        return List.copyOf(bindings.values());
    }

    private static void add(
        LinkedHashMap<String, CarpetManagerBinding> bindings, SettingsManager manager,
        CarpetTranslationRegistry translations
    ) {
        Optional<ModContainer> mod = translations.findMod(manager.identifier());
        String modId = mod.map(container -> container.getMetadata()
                .getId()
            )
            .orElse(null);
        String name = mod.map(container -> container.getMetadata()
                .getName())
            .orElse(manager.identifier());
        bindings.putIfAbsent(manager.identifier(), new CarpetManagerBinding(manager, modId, Component.literal(name)));
    }
}

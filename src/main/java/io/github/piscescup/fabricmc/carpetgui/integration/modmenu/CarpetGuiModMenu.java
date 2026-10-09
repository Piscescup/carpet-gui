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

package io.github.piscescup.fabricmc.carpetgui.integration.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import io.github.piscescup.fabricmc.carpetgui.api.CarpetModMenuApi;
import io.github.piscescup.fabricmc.carpetgui.api.CarpetGuiScreens;
import io.github.piscescup.fabricmc.carpetgui.api.CarpetModInfoApi;
import io.github.piscescup.fabricmc.carpetgui.integration.carpet.CarpetModRegistry;
import net.fabricmc.loader.api.FabricLoader;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mod Menu gives a mod's own factory priority over these automatic fallback factories.
 */
public final class CarpetGuiModMenu
    implements CarpetModMenuApi
{
    @Override
    public String getCarpetSettingsManagerId() {
        return "carpet";
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return CarpetGuiScreens::create;
    }

    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        Map<String, ConfigScreenFactory<?>> factories = new LinkedHashMap<>();
        factories.put("carpet", parent -> CarpetGuiScreens.create(parent, "carpet"));
        for (CarpetModInfoApi info : CarpetModRegistry.discoverInfos()) {
            if (FabricLoader.getInstance()
                .isModLoaded(info.carpetModId())) {
                factories.putIfAbsent(
                    info.carpetModId(),
                    parent -> CarpetGuiScreens.create(parent, info.carpetModId())
                );
            }
        }
        return Map.copyOf(factories);
    }
}

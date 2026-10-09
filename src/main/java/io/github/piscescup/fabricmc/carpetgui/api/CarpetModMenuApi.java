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

package io.github.piscescup.fabricmc.carpetgui.api;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Implement this in a client mod's own "modmenu" entrypoint to open its corresponding Carpet rule page.
 * Only load implementations when Mod Menu is present; use {@link CarpetGuiScreens} for standalone entrypoints.
 */
@FunctionalInterface
public interface CarpetModMenuApi extends ModMenuApi {
    /** Returns SettingsManager.identifier(), which may differ from the Fabric mod ID. */
    String getCarpetSettingsManagerId();

    @Override
    default ConfigScreenFactory<?> getModConfigScreenFactory() {
        String pageId = this instanceof CarpetModInfoApi info ? info.carpetModId() : getCarpetSettingsManagerId();
        return parent -> CarpetGuiScreens.create(parent, pageId);
    }
}

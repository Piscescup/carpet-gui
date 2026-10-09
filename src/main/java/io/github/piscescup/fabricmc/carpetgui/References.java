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

package io.github.piscescup.fabricmc.carpetgui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.piscescup.fabricmc.carpetgui.adapter.PackageRef;
import io.github.piscescup.fabricmc.carpetgui.adapter.CarpetAddonAdapter;
import io.github.piscescup.fabricmc.carpetgui.adapter.codec.CarpetAddonAdapterJsonCodec;
import io.github.piscescup.fabricmc.carpetgui.adapter.Accessor;
import io.github.piscescup.fabricmc.carpetgui.adapter.codec.AccessorJsonCodec;
import io.github.piscescup.fabricmc.carpetgui.adapter.codec.PackageRefJsonCodec;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author REN YuanTong
 * @since
 */
public class References {

    public static final String MOD_ID = "carpet-gui";

    public static final FabricLoader FABRIC_LOADER = FabricLoader.getInstance();

    public static final ModContainer CARPET_GUI_MOD_CONTAINER = FABRIC_LOADER
        .getModContainer(MOD_ID)
        .orElseThrow();

    public static final String MOD_NAME = CARPET_GUI_MOD_CONTAINER
        .getMetadata()
        .getName();

    public static final String MOD_VERSION = CARPET_GUI_MOD_CONTAINER
        .getMetadata()
        .getVersion()
        .getFriendlyString();

    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    public static final Gson GSON = new GsonBuilder()
        .registerTypeHierarchyAdapter(PackageRef.class, new PackageRefJsonCodec())
        .registerTypeHierarchyAdapter(Accessor.class, new AccessorJsonCodec())
        .registerTypeHierarchyAdapter(CarpetAddonAdapter.class, new CarpetAddonAdapterJsonCodec())
        .setPrettyPrinting()
        .create();

    public static final boolean DEBUG = LOGGER.isDebugEnabled();

    public static Identifier fromPath(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}

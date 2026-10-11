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

/**
 * Gson codecs for persisting adapter builder inputs. These codecs store declarative class and
 * member descriptions only; they never serialize live Carpet objects or invoke reflected members.
 *
 * <pre>{@code
 * Gson gson = new GsonBuilder()
 *     .registerTypeAdapter(PackageRef.class, new PackageRefJsonCodec())
 *     .registerTypeAdapter(Accessor.class, new AccessorJsonCodec())
 *     .registerTypeAdapter(CarpetAddonAdapter.class, new CarpetAddonAdapterJsonCodec())
 *     .create();
 * }</pre>
 *
 * @see io.github.piscescup.fabricmc.carpetgui.adapter.codec.AccessorJsonCodec
 * @see io.github.piscescup.fabricmc.carpetgui.adapter.codec.CarpetAddonAdapterJsonCodec
 * @see io.github.piscescup.fabricmc.carpetgui.adapter.codec.PackageRefJsonCodec
 */
package io.github.piscescup.fabricmc.carpetgui.adapter.codec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.piscescup.fabricmc.carpetgui.adapter.Accessor;
import io.github.piscescup.fabricmc.carpetgui.adapter.CarpetAddonAdapter;
import io.github.piscescup.fabricmc.carpetgui.adapter.PackageRef;

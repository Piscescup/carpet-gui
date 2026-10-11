/*
 * This file is part of the Carpet GUI project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  PiscesCup and contributors
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
 * Reflection-based adapters that expose optional Carpet addons through the stable public API
 * without importing addon-specific classes. Builders describe where extension, settings, rule,
 * and manager data lives; resolution is deferred until Carpet GUI requests it.
 *
 * <pre>{@code
 * CarpetAddonAdapter adapter = CarpetAddonAdapter.managerBuilder("my_mod", "dev.example")
 *     .managerClassName("MyExtension")
 *     .managerAccessor(Accessor.ofStaticField("MANAGER"))
 *     .build();
 * }</pre>
 *
 * @see io.github.piscescup.fabricmc.carpetgui.adapter.CarpetAddonAdapter
 * @see io.github.piscescup.fabricmc.carpetgui.adapter.Accessor
 * @see io.github.piscescup.fabricmc.carpetgui.api.CarpetModRulesApi
 */
package io.github.piscescup.fabricmc.carpetgui.adapter;

import io.github.piscescup.fabricmc.carpetgui.adapter.Accessor;
import io.github.piscescup.fabricmc.carpetgui.adapter.CarpetAddonAdapter;

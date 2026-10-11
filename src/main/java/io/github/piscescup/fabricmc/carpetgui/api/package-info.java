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
 * Public contracts used to expose mods, rule data, editing backends, and browser data sources to
 * Carpet GUI without coupling presentation code to a particular Carpet implementation.
 *
 * <pre>{@code
 * RuleSource source = ...;
 * for (RuleView rule : source.vanillaRules()) {
 *     if (rule instanceof EditableRuleView editable) {
 *         editable.editor().submit("true");
 *     }
 * }
 * }</pre>
 *
 * @see io.github.piscescup.fabricmc.carpetgui.api.CarpetModInfoApi
 * @see io.github.piscescup.fabricmc.carpetgui.api.RuleView
 * @see io.github.piscescup.fabricmc.carpetgui.api.RuleSource
 */
package io.github.piscescup.fabricmc.carpetgui.api;

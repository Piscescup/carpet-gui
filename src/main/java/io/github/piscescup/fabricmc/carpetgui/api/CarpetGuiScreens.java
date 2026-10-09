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

import io.github.piscescup.fabricmc.carpetgui.gui.workspace.CarpetWorkspaceScreen;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleSource;
import io.github.piscescup.fabricmc.carpetgui.integration.carpet.CarpetRuleSource;
import net.minecraft.client.gui.screens.Screen;

/**
 * Public factories, usable without Mod Menu. Prefer the Fabric mod ID for the selected page.
 */
public final class CarpetGuiScreens {
    private CarpetGuiScreens() {
    }

    public static Screen create(Screen parent) {
        return new CarpetWorkspaceScreen(parent, new CarpetRuleSource(), null).getAsScreen();
    }

    public static Screen create(Screen parent, String modId) {
        CarpetRuleSource source = new CarpetRuleSource();
        return new CarpetWorkspaceScreen(parent, source, source.resolvePageId(modId)).getAsScreen();
    }

    /** Creates the same workspace for a custom rule system; null initialPageId opens Home. */
    public static Screen create(Screen parent, RuleSource source, String initialPageId) {
        return new CarpetWorkspaceScreen(parent, source, initialPageId).getAsScreen();
    }
}

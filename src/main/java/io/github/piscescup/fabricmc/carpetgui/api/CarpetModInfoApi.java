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

import io.github.piscescup.fabricmc.carpetgui.resources.CarpetTranslationResources;

import java.util.Map;
import java.util.Set;

/** Identity of a Carpet mod page. Register under {@value #ENTRYPOINT}, or implement on a registered CarpetExtension. */
public interface CarpetModInfoApi {
    String ENTRYPOINT = "carpet-gui-mods";

    /** Actual Fabric mod ID; it is not necessarily the rule manager's command root. */
    String carpetModId();

    /** Displayed verbatim on the first-row tab, e.g. Carpet Mod or Carpet Igny Addition. */
    String carpetFancyName();

    default String getModId() {
        return carpetModId();
    }

    /**
     * Explicit ownership of an entire settings manager. Shared managers should use individual rule ownership instead.
     * Values must match SettingsManager.identifier(); empty declares no manager ownership; rule providers can claim individual rules.
     */
    default Set<String> getSettingsManagerIds() {
        return Set.of();
    }

    /**
     * Optional translations in Carpet's original key format: manager.rule.ruleId.desc,
     * manager.rule.ruleId.extra.N and manager.category.categoryId.
     * By default, reads this mod's own {@code assets/<namespace>/lang} resources at runtime.
     * Override for a custom resource layout or to delegate to CarpetExtension.canHasTranslations.
     * Nothing is copied into Carpet GUI's language files. Implementations must not change global Carpet language.
     */
    default Map<String, String> getTranslations(String language) {
        return CarpetTranslationResources.read(getModId(), language);
    }
}

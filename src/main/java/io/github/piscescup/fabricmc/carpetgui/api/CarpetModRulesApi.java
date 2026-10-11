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

import carpet.api.settings.CarpetRule;
import carpet.api.settings.SettingsManager;

import java.util.Collection;


/**
 * Adds explicit ownership of live, registered Carpet rules to {@link CarpetModInfoApi}.
 * This capability is useful for addons that share another mod's {@code SettingsManager} and
 * therefore cannot claim the entire manager through {@link #getSettingsManagerIds()}.
 * A provider may expose rules from more than one manager. Implementations do not need to expose
 * their manager lookup or rule-name discovery strategy.
 *
 * <pre>{@code
 * public Collection<CarpetRule<?>> asCarpetRules() {
 *     if (manager == null) return List.of();
 *     CarpetRule<?> rule = manager.getCarpetRule("myRule");
 *     return rule == null ? List.of() : List.of(rule);
 * }
 * }</pre>
 *
 * @see CarpetModInfoApi
 * @see carpet.api.settings.SettingsManager
 */
public interface CarpetModRulesApi extends CarpetModInfoApi {

    /**
     * Returns the live Carpet rules owned by this mod. Rules must be the actual instances
     * registered with their managers, not copies or wrappers. This method may be called before a
     * world is loaded; return an empty collection while rules are unavailable.
     *
     * <pre>{@code
     * for (CarpetRule<?> rule : api.asCarpetRules()) {
     *     registerRuleOwner(rule, api.carpetModId());
     * }
     * }</pre>
     *
     * @return a non-null collection of owned rules, without null elements; empty while unavailable
     * @throws RuntimeException if the provider cannot read its rules; the GUI logs the failure
     *         and skips this provider's rules for that discovery pass
     * @see SettingsManager#getCarpetRules()
     * @see SettingsManager#getCarpetRule(String)
     */
    Collection<CarpetRule<?>> asCarpetRules();

}

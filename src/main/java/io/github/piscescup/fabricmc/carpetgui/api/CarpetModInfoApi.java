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

import io.github.piscescup.fabricmc.carpetgui.util.CarpetTranslationUtils;

import java.util.Map;
import java.util.Set;

/**
 * Supplies the identity, manager ownership, and translations used to build a Carpet mod page.
 * Implementations may be registered under the Fabric entrypoint {@value #ENTRYPOINT}, or the
 * interface may be implemented by an already registered Carpet extension.
 *
 * <pre>{@code
 * public final class MyModInfo implements CarpetModInfoApi {
 *     public String carpetModId() { return "my_mod"; }
 *     public String carpetFancyName() { return "My Carpet Addon"; }
 * }
 * }</pre>
 *
 * @see CarpetModRulesApi
 * @see io.github.piscescup.fabricmc.carpetgui.adapter.CarpetAddonAdapter
 * @see carpet.CarpetExtension
 */
public interface CarpetModInfoApi {
    /** Fabric entrypoint key used to discover implementations of this API. */
    String ENTRYPOINT = "carpet-gui-mods";

    /**
     * Returns the actual Fabric mod identifier; this is not necessarily the rule manager's
     * command root.
     *
     * <pre>{@code
     * String namespace = api.carpetModId(); // e.g. "my_mod"
     * }</pre>
     *
     * @return the Fabric mod ID used for ownership and resource lookup
     */
    String carpetModId();

    /**
     * Returns the name displayed verbatim on the first-row tab.
     *
     * <pre>{@code
     * Component title = Component.literal(api.carpetFancyName());
     * }</pre>
     *
     * @return a human-readable name, such as {@code "Carpet Mod"}
     */
    String carpetFancyName();

    /**
     * Returns the mod identifier through the conventional getter name.
     *
     * <pre>{@code
     * assert api.getModId().equals(api.carpetModId());
     * }</pre>
     *
     * @return the same value as {@link #carpetModId()}
     */
    default String getModId() {
        return carpetModId();
    }

    /**
     * Declares explicit ownership of complete Carpet settings managers. Values must match
     * {@code SettingsManager.identifier()}; an empty set declares no manager ownership. Addons
     * sharing a manager should instead implement {@link CarpetModRulesApi} and claim individual
     * rules.
     *
     * <pre>{@code
     * public Set<String> getSettingsManagerIds() {
     *     return Set.of("myManager");
     * }
     * }</pre>
     *
     * @return the identifiers of managers wholly owned by this mod; empty by default
     * @see carpet.api.settings.SettingsManager#identifier()
     */
    default Set<String> getSettingsManagerIds() {
        return Set.of();
    }

    /**
     * Returns optional translations using Carpet's original key format, including
     * {@code manager.rule.ruleId.desc}, {@code manager.rule.ruleId.extra.N}, and
     * {@code manager.category.categoryId}. The default implementation reads this mod's own
     * {@code assets/<namespace>/lang} resource at runtime. Implementations may override this for a
     * custom resource layout or to delegate to {@code CarpetExtension.canHasTranslations}; they
     * must not change Carpet's global language state.
     *
     * <pre>{@code
     * Map<String, String> zhCn = api.getTranslations("zh_cn");
     * String description = zhCn.get("myManager.rule.myRule.desc");
     * }</pre>
     *
     * @param language the Minecraft language code, for example {@code "en_us"} or {@code "zh_cn"}
     * @return an immutable-compatible mapping of Carpet translation keys to translated text;
     *         empty when no translations are available
     * @throws RuntimeException if an overriding implementation cannot load or construct its map
     * @see CarpetTranslationUtils#read(String, String)
     */
    default Map<String, String> getTranslations(String language) {
        return CarpetTranslationUtils.read(getModId(), language);
    }
}

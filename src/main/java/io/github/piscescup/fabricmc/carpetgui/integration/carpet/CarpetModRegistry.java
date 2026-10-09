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

package io.github.piscescup.fabricmc.carpetgui.integration.carpet;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import io.github.piscescup.fabricmc.carpetgui.adapter.CarpetAddonAdapter;
import carpet.api.settings.CarpetRule;
import io.github.piscescup.fabricmc.carpetgui.api.CarpetModInfoApi;
import io.github.piscescup.fabricmc.carpetgui.api.CarpetModRulesApi;
import net.fabricmc.loader.api.FabricLoader;

import java.util.*;

import static io.github.piscescup.fabricmc.carpetgui.References.LOGGER;
import static io.github.piscescup.fabricmc.carpetgui.integration.carpet.CarpetSeriesMods.CARPET_SERIES;

/** Separates Fabric mod/page identity from Carpet settings-manager identity. */
public final class CarpetModRegistry {

    private CarpetModRegistry() {
    }

    private record RuleKey(String manager, String name) {
        private static RuleKey of(CarpetRule<?> rule) {
            return new RuleKey(rule.settingsManager().identifier(), rule.name());
        }
    }

    private record Providers(Map<String, CarpetModInfoApi> infos, Map<String, CarpetModRulesApi> rules) {
    }

    /** Can be called by Mod Menu before the game/world initializes rules. */
    public static List<CarpetModInfoApi> discoverInfos() {
        return List.copyOf(
            providers(CarpetTranslationRegistry.discover())
                .infos()
                .values()
        );
    }

    private static Providers providers(CarpetTranslationRegistry translations) {
        FabricLoader loader = FabricLoader.getInstance();

        Map<String, CarpetModInfoApi> infos = new LinkedHashMap<>();
        Map<String, CarpetModRulesApi> rules = new LinkedHashMap<>();

        for (String id : translations.modIds()) {
            translations.findProvider(id).ifPresent(info -> infos.putIfAbsent(id, info));
        }

        List<CarpetExtension> extensions = List.copyOf(CarpetServer.extensions);

        Map<String, CarpetModInfoApi> declared =
            declaredProviders(loader, extensions);

        for (Map.Entry<String, CarpetModInfoApi> entry : declared.entrySet()) {
            String id = entry.getKey();
            CarpetModInfoApi info = entry.getValue();
            String name = info.carpetFancyName();

            if (id == null || id.isBlank()
                || !loader.isModLoaded(id)
                || name == null || name.isBlank()) {
                throw new IllegalArgumentException(
                    "Invalid Carpet mod info provider: " + id
                );
            }

            infos.put(id, info);
            translations.registerProviderIfAbsent(info);

            if (info instanceof CarpetModRulesApi provider) {
                rules.put(id, provider);
                translations.registerProviderIfAbsent(provider);
            }

        }

        registerCarpetSeriesMods(
            loader,
            translations,
            infos,
            rules,
            declared
        );
        return new Providers(infos, rules);
    }

    private static Map<String, CarpetModInfoApi> declaredProviders(
        FabricLoader loader,
        List<CarpetExtension> extensions
    ) {
        Map<String, CarpetModInfoApi> declared = new LinkedHashMap<>();
        for (CarpetModInfoApi info : loader.getEntrypoints(CarpetModInfoApi.ENTRYPOINT, CarpetModInfoApi.class)) {
            String id = info.carpetModId();
            if (declared.putIfAbsent(id, info) != null) {
                throw new IllegalArgumentException("Duplicate Carpet mod info for " + id);
            }
        }
        for (CarpetExtension extension : extensions) {
            if (extension instanceof CarpetModInfoApi info) declared.putIfAbsent(info.carpetModId(), info);
        }
        return declared;
    }

    private static void registerCarpetSeriesMods(
        FabricLoader loader,
        CarpetTranslationRegistry translations,
        Map<String, CarpetModInfoApi> infos,
        Map<String, CarpetModRulesApi> rules,
        Map<String, CarpetModInfoApi> declared
    ) {
        for (CarpetModRulesApi carpet : CARPET_SERIES) {
            String id = carpet.carpetModId();
            if (!loader.isModLoaded(id) || rules.containsKey(id)) continue;
            try {
                if (!id.equals(carpet.carpetModId())) {
                    throw new IllegalArgumentException(
                        "Addon class belongs to " + carpet.carpetModId() + ", expected " + id);
                }
                if (!declared.containsKey(id)) {
                    infos.put(id, carpet);
                }
                rules.put(id, carpet);
                translations.registerProviderIfAbsent(carpet);
            } catch (RuntimeException | LinkageError failure) {
                LOGGER.warn("Skipping incompatible optional Carpet addon adapter for {}", id, failure);
            }
        }

    }

    /** Discovers pages and assigns every registered rule to at most one Fabric mod page. */
    public static List<CarpetModBinding> discover(CarpetTranslationRegistry translations) {
        Providers providers = providers(translations);
        List<CarpetManagerBinding> managers = CarpetManagerBinding.discover(translations);
        Map<String, CarpetModInfoApi> infos = new LinkedHashMap<>(providers.infos());
        Map<RuleKey, CarpetRule<?>> registered = new LinkedHashMap<>();
        Map<RuleKey, String> owners = new LinkedHashMap<>();

        for (CarpetManagerBinding binding : managers) {
            addManagerInfoIfAbsent(infos, binding);
            registerManagerRules(binding, registered, owners);
        }

        Map<RuleKey, String> defaultOwners = new LinkedHashMap<>(owners);
        Map<RuleKey, String> claimed = new LinkedHashMap<>();
        for (Map.Entry<String, CarpetModRulesApi> entry : providers.rules().entrySet()) {
            String providerId = entry.getKey();
            for (CarpetRule<?> rule : providerRules(providerId, entry.getValue())) {
                claimRule(providerId, rule, registered, owners, defaultOwners, claimed);
            }
        }

        Map<String, List<CarpetRule<?>>> grouped = new LinkedHashMap<>();
        registered.forEach((key, rule) -> grouped.computeIfAbsent(owners.get(key), ignored -> new ArrayList<>()).add(rule));

        List<CarpetModBinding> pages = new ArrayList<>();
        infos.forEach((id, info) -> pages.add(new CarpetModBinding(info, grouped.getOrDefault(id, List.of()))));
        return List.copyOf(pages);
    }

    private static void addManagerInfoIfAbsent(
        Map<String, CarpetModInfoApi> infos,
        CarpetManagerBinding binding
    ) {
        String id = binding.modId();
        if (id == null || id.isBlank() || infos.containsKey(id)) return;
        String name = binding.title().getString();
        String displayName = name.isBlank() ? id : name;
        infos.put(id, new CarpetModInfoApi() {
            @Override
            public String carpetModId() {
                return id;
            }

            @Override
            public String carpetFancyName() {
                return displayName;
            }
        });
    }

    private static void registerManagerRules(
        CarpetManagerBinding binding,
        Map<RuleKey, CarpetRule<?>> registered,
        Map<RuleKey, String> owners
    ) {
        Collection<CarpetRule<?>> rules;
        try {
            rules = binding.manager().getCarpetRules();
        } catch (RuntimeException | LinkageError failure) {
            LOGGER.warn("Cannot read Carpet rules from manager {}", binding.manager().identifier(), failure);
            return;
        }
        if (rules == null) {
            LOGGER.warn("Carpet manager {} returned a null rule collection", binding.manager().identifier());
            return;
        }
        for (CarpetRule<?> rule : rules) {
            if (rule == null || rule.settingsManager() == null) continue;
            RuleKey key = RuleKey.of(rule);
            CarpetRule<?> previous = registered.putIfAbsent(key, rule);
            if (previous != null && previous != rule) {
                LOGGER.warn("Ignoring duplicate registered Carpet rule instance {}/{}", key.manager(), key.name());
                continue;
            }
            owners.putIfAbsent(key, binding.modId());
        }
    }

    private static List<CarpetRule<?>> providerRules(String providerId, CarpetModRulesApi provider) {
        try {
            Collection<CarpetRule<?>> rules = provider.asCarpetRules();
            if (rules == null) {
                LOGGER.warn("Carpet rule provider {} returned a null rule collection", providerId);
                return List.of();
            }
            return new ArrayList<>(rules);
        } catch (RuntimeException | LinkageError failure) {
            LOGGER.warn("Cannot read Carpet rules from provider {}", providerId, failure);
            return List.of();
        }
    }

    private static void claimRule(
        String providerId,
        CarpetRule<?> rule,
        Map<RuleKey, CarpetRule<?>> registered,
        Map<RuleKey, String> owners,
        Map<RuleKey, String> defaultOwners,
        Map<RuleKey, String> claimed
    ) {
        if (rule == null || rule.settingsManager() == null) return;
        RuleKey key = RuleKey.of(rule);
        if (registered.get(key) != rule) {
            LOGGER.warn("Ignoring unregistered rule {}/{} from {}", key.manager(), key.name(), providerId);
            return;
        }

        String previous = claimed.putIfAbsent(key, providerId);
        if (previous != null && !previous.equals(providerId)) {
            // A manager owner's broad list is a fallback; a specific addon owns its shared rules.
            if (providerId.equals(defaultOwners.get(key))) return;
            if (!previous.equals(defaultOwners.get(key))) {
                // Addons sharing Carpet's root manager can legitimately use the same rule name.
                // Reflection resolves both declarations to the one live rule instance, so keep
                // the first specific owner instead of making an optional compatibility clash fatal.
                LOGGER.warn(
                    "Rule {}/{} is exposed by both {} and {}; keeping {}",
                    key.manager(), key.name(), previous, providerId, previous
                );
                return;
            }
            claimed.put(key, providerId);
        }
        owners.put(key, providerId);
    }
}

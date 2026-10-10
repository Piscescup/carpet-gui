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
import com.google.common.collect.Maps;
import io.github.piscescup.fabricmc.carpetgui.api.CarpetModInfoApi;
import io.github.piscescup.fabricmc.carpetgui.resources.CarpetTranslationResources;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Runtime providers and translation caches are isolated by Fabric mod ID. No datagen snapshots.
 */
public final class CarpetTranslationRegistry {
    private final List<ModContainer> mods;
    private final Map<String, Source> sources = Maps.newConcurrentMap();
    private final Map<String, String> managerOwners = Maps.newConcurrentMap();
    private final Map<LanguageKey, Map<String, String>> languageCache = Maps.newConcurrentMap();

    private record LanguageKey(
        String modId,
        String language
    ) {
    }

    /**
     * Combines translations for a mod explicitly registered through the API or an adapter.
     */
    private static final class Source
        implements CarpetModInfoApi
    {
        private final ModContainer mod;
        private CarpetModInfoApi explicit;
        private final List<CarpetExtension> extensions = new ArrayList<>();

        private Source(ModContainer mod) {
            this.mod = mod;
        }

        @Override
        public String carpetModId() {
            return mod.getMetadata().getId();
        }

        @Override
        public String carpetFancyName() {
            return explicit == null ? mod.getMetadata().getName() : explicit.carpetFancyName();
        }

        @Override
        public Set<String> getSettingsManagerIds() {
            return explicit == null ? Set.of() : explicit.getSettingsManagerIds();
        }

        @Override
        public Map<String, String> getTranslations(String language) {
            Map<String, String> translations = new LinkedHashMap<>();
            if (explicit != null)
                CarpetTranslationResources.merge(translations, explicit.getTranslations(language));

            for (CarpetExtension extension : extensions) {
                CarpetTranslationResources.merge(translations, extension.canHasTranslations(language));
            }
            CarpetTranslationResources.merge(translations, CarpetTranslationResources.read(mod, language));
            return Map.copyOf(translations);
        }
    }

    public static CarpetTranslationRegistry discover() {
        FabricLoader loader = FabricLoader.getInstance();
        return new CarpetTranslationRegistry(
            loader.getAllMods(),
            loader.getEntrypoints(CarpetModInfoApi.ENTRYPOINT, CarpetModInfoApi.class),
            List.copyOf(CarpetServer.extensions)
        );
    }

    public CarpetTranslationRegistry(
        Collection<ModContainer> mods,
        Collection<CarpetModInfoApi> providers,
        Collection<CarpetExtension> extensions
    ) {
        this.mods = List.copyOf(mods);
        for (CarpetModInfoApi provider : providers) register(provider);
        // Carpet itself is a built-in API provider, with an explicit Fabric identity.
        if (findById("carpet").isPresent()) {
            registerProviderIfAbsent(new CarpetModInfoApi() {
                @Override
                public String carpetModId() { return "carpet"; }

                @Override
                public String carpetFancyName() { return "Carpet Mod"; }

                @Override
                public Set<String> getSettingsManagerIds() {
                    return Set.of(CarpetServer.settingsManager == null
                        ? "carpet" : CarpetServer.settingsManager.identifier());
                }
            });
            claimManager(CarpetServer.settingsManager == null
                ? "carpet" : CarpetServer.settingsManager.identifier(), "carpet");
        }
        for (CarpetExtension extension : extensions) {
            if (!(extension instanceof CarpetModInfoApi provider)) continue;
            registerProviderIfAbsent(provider);
            findById(provider.carpetModId()).ifPresent(mod -> source(mod).extensions.add(extension));
            var manager = extension.extensionSettingsManager();
            if (manager != null && manager != CarpetServer.settingsManager) {
                claimManager(manager.identifier(), provider.carpetModId());
            }
        }
        for (var adapter : CarpetSeriesMods.CARPET_SERIES) {
            if (findById(adapter.carpetModId()).isPresent()) registerProviderIfAbsent(adapter);
        }
    }


    private Source source(ModContainer mod) {
        return sources.computeIfAbsent(
            mod.getMetadata()
                .getId(),
            ignored -> new Source(mod)
        );
    }

    private void register(CarpetModInfoApi provider) {
        String id = Objects.requireNonNull(provider.getModId(), "Translation provider Mod ID");
        ModContainer mod = findById(id)
            .orElseThrow(() -> new IllegalArgumentException(
                "Carpet translation provider refers to an unloaded mod: " + id)
            );
        Source source = source(mod);
        if (source.explicit != null && source.explicit != provider) {
            throw new IllegalArgumentException("Duplicate Carpet mod provider for mod " + id);
        }
        source.explicit = provider;
        for (String managerId : provider.getSettingsManagerIds()) claimManager(managerId, id);
    }

    private void claimManager(String managerId, String modId) {
        if (managerId == null || managerId.isBlank()) throw new IllegalArgumentException("Empty Carpet manager ID");
        String previous = managerOwners.putIfAbsent(managerId, modId);
        if (previous != null && !previous.equals(modId)) {
            throw new IllegalArgumentException("Carpet manager " + managerId + " claimed by both " + previous + " and " + modId);
        }
    }

    private Optional<ModContainer> findById(String id) {
        return mods.stream()
            .filter(mod -> mod.getMetadata()
                .getId()
                .equals(id))
            .findFirst();
    }

    public Optional<ModContainer> findMod(String managerId) {
        String modId = managerOwners.get(managerId);
        return modId == null ? Optional.empty() : findById(modId);
    }

    /**
     * Returns the runtime translation API for this mod, registered through the API or an adapter.
     */
    public Optional<CarpetModInfoApi> findProvider(String modId) {
        return Optional.ofNullable(sources.get(modId));
    }

    public List<String> modIds() {
        return List.copyOf(sources.keySet());
    }

    /** A dedicated translation entrypoint wins over an info/rules entrypoint's default translations. */
    public void registerProviderIfAbsent(CarpetModInfoApi provider) {
        Source source = sources.get(provider.getModId());
        if (source == null || source.explicit == null) {
            register(provider);
            languageCache.clear();
        }
    }

    /**
     * Reads only this mod's provider; unrelated mods cannot fill or override its keys.
     */
    public synchronized Map<String, String> getTranslations(String modId, String language) {
        return languageCache.computeIfAbsent(
            new LanguageKey(modId, language),
            key -> findProvider(key.modId())
                .map(api -> api.getTranslations(key.language()))
                .orElseGet(Map::of)
        );
    }
}

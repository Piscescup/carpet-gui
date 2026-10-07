package io.github.piscescup.fabricmc.carpetgui.integration.carpet;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import io.github.piscescup.fabricmc.carpetgui.api.CarpetModInfoApi;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
    private final Map<String, Source> sources = new LinkedHashMap<>();
    private final Map<String, String> managerOwners = new HashMap<>();
    private final Map<String, CarpetExtension> unownedExtensions = new HashMap<>();
    private final Map<LanguageKey, Map<String, String>> languageCache = new HashMap<>();

    private record LanguageKey(
        String modId,
        String language
    ) {
    }

    /**
     * Adapts installed Carpet mods to the public API, including those not implementing it themselves.
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
            if (explicit != null) CarpetTranslationResources.merge(translations, explicit.getTranslations(language));
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
        CarpetModLookup.find(this.mods, CarpetServer.class.getName(), "carpet")
            .ifPresent(mod -> {
                source(mod);
                String managerId = CarpetServer.settingsManager == null ? "carpet" : CarpetServer.settingsManager.identifier();
                managerOwners.putIfAbsent(
                    managerId,
                    mod.getMetadata()
                        .getId()
                );
            });
        for (CarpetExtension extension : extensions) {
            if (extension instanceof CarpetModInfoApi provider) {
                Source existing = sources.get(provider.getModId());
                if (existing == null || existing.explicit == null) register(provider);
            }
            var manager = extension.extensionSettingsManager();
            String managerId = manager == null ? "" : manager.identifier();
            Optional<ModContainer> mod = extension instanceof CarpetModInfoApi provider
                ? findById(provider.getModId()) : CarpetModLookup.find(this.mods, extension.getClass(), "")
                    .or(() -> findMod(extension.getClass(), managerId));
            mod.ifPresent(owner -> source(owner).extensions.add(extension));
            if (manager == null) continue;
            if (extension instanceof CarpetModInfoApi provider) {
                claimManager(managerId, provider.getModId());
            } else if (mod.isPresent()) {
                managerOwners.putIfAbsent(
                    managerId,
                    mod.get()
                        .getMetadata()
                        .getId()
                );
            } else {
                // Preserve Carpet's translation API for wrapper mods whose jar cannot be located.
                unownedExtensions.putIfAbsent(managerId, extension);
            }
        }
    }


    private Source source(ModContainer mod) {
        return sources.computeIfAbsent(
            mod.getMetadata()
                .getId(), ignored -> new Source(mod)
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

    public Optional<ModContainer> findMod(Class<?> owner, String managerId) {
        String modId = managerOwners.get(managerId);
        return modId == null ? CarpetModLookup.find(mods, owner, managerId) : findById(modId);
    }

    /**
     * Returns the runtime translation API for this mod, including automatically adapted extensions.
     */
    public Optional<CarpetModInfoApi> findProvider(String modId) {
        return Optional.ofNullable(sources.get(modId));
    }

    public List<String> modIds() {
        return List.copyOf(sources.keySet());
    }

    public void registerMod(String modId) {
        source(findById(modId).orElseThrow(() -> new IllegalArgumentException("Unloaded Carpet mod: " + modId)));
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
            new LanguageKey(modId, language), key ->
                findProvider(key.modId()).map(api -> api.getTranslations(key.language()))
                    .orElseGet(Map::of)
        );
    }

    public synchronized String translation(String managerId, String language, String key) {
        String modId = managerOwners.get(managerId);
        if (modId != null) return getTranslations(modId, language).get(key);
        CarpetExtension extension = unownedExtensions.get(managerId);
        if (extension == null) return null;
        Map<String, String> translations = languageCache.computeIfAbsent(
            new LanguageKey("@" + managerId, language), ignored -> {
                Map<String, String> result = new LinkedHashMap<>();
                CarpetTranslationResources.merge(result, extension.canHasTranslations(language));
                return Map.copyOf(result);
            }
        );
        return translations.get(key);
    }
}

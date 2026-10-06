package io.github.piscescup.fabricmc.carpetgui.integration.carpet;

import carpet.utils.Translations;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Finds actual resource namespaces in a mod instead of assuming they match its mod/manager ID.
 */
public final class CarpetTranslationResources {
    private CarpetTranslationResources() {
    }

    public static Map<String, String> read(String modId, String language) {
        return FabricLoader.getInstance()
            .getModContainer(modId)
            .map(mod -> read(mod, language))
            .orElseGet(Map::of);
    }

    public static Map<String, String> read(ModContainer mod, String language) {
        return mod.findPath("assets")
            .map(assets -> readAssets(assets, language))
            .orElseGet(Map::of);
    }

    private static Map<String, String> readAssets(Path assets, String language) {
        if (!Files.isDirectory(assets)) return Map.of();
        Map<String, String> translations = new LinkedHashMap<>();
        try (var namespaces = Files.list(assets)) {
            namespaces.filter(Files::isDirectory)
                .sorted()
                .filter(namespace -> Files.isRegularFile(namespace.resolve("lang")
                    .resolve(language + ".json"))
                )
                .forEach(namespace -> merge(
                        translations, Translations.getTranslationFromResourcePath(
                            "assets/" + namespace.getFileName() + "/lang/" + language + ".json"
                        )
                    )
                );
            return Map.copyOf(translations);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to discover Carpet translation resources in " + assets, exception);
        }
    }

    static void merge(Map<String, String> target, Map<String, String> source) {
        if (source == null) return;
        source.forEach((key, value) -> {
            if (key == null || key.startsWith("//") || value == null || value.isBlank()) return;
            // Carpet's historical translation key format, not a deprecated Java API.
            String normalized = key.startsWith("rule.") || key.startsWith("category.") ? "carpet." + key : key;
            target.putIfAbsent(normalized, value);
        });
    }
}

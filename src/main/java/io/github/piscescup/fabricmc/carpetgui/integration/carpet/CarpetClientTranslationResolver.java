package io.github.piscescup.fabricmc.carpetgui.integration.carpet;

import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Client-language presentation only; the registry and public API remain common-side. */
public final class CarpetClientTranslationResolver {
    private final CarpetTranslationRegistry registry;

    public CarpetClientTranslationResolver(CarpetTranslationRegistry registry) {
        this.registry = registry;
    }

    private String current(String modId, String key) {
        String language = Minecraft.getInstance().getLanguageManager().getSelected();
        return registry.getTranslations(modId, language).get(key);
    }

    public Component resolve(String modId, String sourceKey, String apiFallback) {
        String current = current(modId, sourceKey);
        String english = current == null ? registry.getTranslations(modId, "en_us").get(sourceKey) : null;
        String fallback = current != null ? current : english != null ? english : apiFallback;
        // Resource packs/source language keys always win over programmatic fallback text.
        return Component.translatableWithFallback(sourceKey, fallback);
    }

    public boolean has(String modId, String sourceKey) {
        var language = Language.getInstance();
        return language.has(sourceKey) || current(modId, sourceKey) != null
            || registry.getTranslations(modId, "en_us").get(sourceKey) != null;
    }

    /** Reads original addon translations for search without changing the selected/global language. */
    public List<String> searchTerms(String modId, String rulePrefix) {
        List<String> terms = new ArrayList<>();
        for (String language : List.of("en_us", "zh_cn", "zh_tw")) {
            registry.getTranslations(modId, language).forEach((key, value) -> {
                if (key.startsWith(rulePrefix)) terms.add(value);
            });
        }
        return List.copyOf(terms);
    }
}

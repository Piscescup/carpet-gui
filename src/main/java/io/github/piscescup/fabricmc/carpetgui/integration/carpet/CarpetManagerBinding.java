package io.github.piscescup.fabricmc.carpetgui.integration.carpet;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.api.settings.SettingsManager;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/**
 * Manager IDs (command roots) and Fabric mod IDs are intentionally kept distinct.
 */
public record CarpetManagerBinding(
    SettingsManager manager,
    String modId,
    Component title
) {
    public static List<CarpetManagerBinding> discover() {
        return discover(CarpetTranslationRegistry.discover());
    }

    public static List<CarpetManagerBinding> discover(CarpetTranslationRegistry translations) {
        var bindings = new LinkedHashMap<String, CarpetManagerBinding>();
        if (CarpetServer.settingsManager != null) add(bindings, CarpetServer.settingsManager, CarpetServer.class, translations);
        for (CarpetExtension extension : List.copyOf(CarpetServer.extensions)) {
            SettingsManager manager = extension.extensionSettingsManager();
            if (manager != null) add(bindings, manager, extension.getClass(), translations);
        }
        return List.copyOf(bindings.values());
    }

    private static void add(
        LinkedHashMap<String, CarpetManagerBinding> bindings, SettingsManager manager, Class<?> owner,
        CarpetTranslationRegistry translations
    ) {
        Optional<ModContainer> mod = translations.findMod(owner, manager.identifier());
        String modId = mod.map(container -> container.getMetadata()
                .getId()
            )
            .orElse(manager.identifier());
        String name = mod.map(container -> container.getMetadata()
                .getName())
            .orElse(manager.identifier());
        bindings.putIfAbsent(manager.identifier(), new CarpetManagerBinding(manager, modId, Component.literal(name)));
    }
}

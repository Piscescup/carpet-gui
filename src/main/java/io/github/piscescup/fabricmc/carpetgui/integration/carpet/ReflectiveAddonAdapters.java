package io.github.piscescup.fabricmc.carpetgui.integration.carpet;

import carpet.CarpetExtension;
import carpet.api.settings.CarpetRule;
import io.github.piscescup.fabricmc.carpetgui.api.CarpetAddonAdapter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static io.github.piscescup.fabricmc.carpetgui.References.LOGGER;

/** Automatic extension metadata discovery plus compatibility for known shared-manager rule sources. */
final class ReflectiveAddonAdapters {
    private ReflectiveAddonAdapters() {}

    private static Class<?> load(String name) throws ClassNotFoundException {
        return Class.forName(name, false, ReflectiveAddonAdapters.class.getClassLoader());
    }

    record ExtensionInfo(String modId, String fancyName, CarpetExtension extension) {}

    /** Discover every registered extension from its runtime class, without a new addon entrypoint. */
    static List<ExtensionInfo> discover() {
        List<ExtensionInfo> result = new ArrayList<>();
        var mods = net.fabricmc.loader.api.FabricLoader.getInstance().getAllMods();
        for (CarpetExtension extension : List.copyOf(carpet.CarpetServer.extensions)) {
            var manager = extension.extensionSettingsManager();
            String managerId = manager == null ? "" : manager.identifier();
            // A shared base manager cannot identify the addon; do not use it as an ID fallback.
            if (manager == carpet.CarpetServer.settingsManager) managerId = "";
            var mod = CarpetModLookup.find(mods, extension.getClass(), managerId);
            if (mod.isEmpty()) continue;
            var metadata = mod.get().getMetadata();
            result.add(new ExtensionInfo(metadata.getId(), displayName(extension, metadata.getName()), extension));
        }
        return List.copyOf(result);
    }

    static String displayName(Object extension, String fallback) {
        Class<?> type = extension.getClass();
        for (String name : List.of("carpetFancyName", "getFancyName", "getModName")) {
            try {
                var method = type.getMethod(name);
                if (method.getReturnType() != String.class) continue;
                Object receiver = java.lang.reflect.Modifier.isStatic(method.getModifiers()) ? null : extension;
                String value = (String) method.invoke(receiver);
                if (value != null && !value.isBlank()) return value;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                // Public metadata conventions are optional.
            }
        }
        for (String name : List.of("fancyName", "FANCY_NAME", "MOD_NAME")) {
            try {
                var field = type.getField(name);
                if (field.getType() != String.class) continue;
                Object receiver = java.lang.reflect.Modifier.isStatic(field.getModifiers()) ? null : extension;
                String value = (String) field.get(receiver);
                if (value != null && !value.isBlank()) return value;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                // Fall back to Fabric's real metadata display name.
            }
        }
        return fallback;
    }

    static CarpetAddonAdapter pry() throws ReflectiveOperationException {
        Method instance = load("me.primaryuan.carpet.CarpetPrimaryuanServer").getMethod("getInstance");
        return CarpetAddonAdapter.fromSettingsClass("carpet-pry-addition", "carpet",
                "me.primaryuan.carpet.CarpetPrimaryuanSettings", "me.primaryuan.carpet.settings.Rule")
            .withTranslations(language -> translations("carpet-pry-addition", instance, language));
    }

    static CarpetAddonAdapter igny() throws ReflectiveOperationException {
        Method listRules = load("com.liuyue.igny.IGNYSettings").getMethod("listRules");
        Method rule = load("com.liuyue.igny.rule.RuleContext").getMethod("rule");
        Class<?> server = load("com.liuyue.igny.IGNYServer");
        Method instance = server.getMethod("getInstance");
        String fancyName = (String) server.getField("fancyName").get(null);
        return CarpetAddonAdapter.fromRules("carpet-igny-addition", "carpet", () -> {
            try {
                if (!(listRules.invoke(null) instanceof Collection<?> contexts)) {
                    throw new IllegalStateException("IGNYSettings.listRules() did not return a collection");
                }
                List<CarpetRule<?>> rules = new ArrayList<>();
                for (Object context : contexts) {
                    Object value = rule.invoke(context);
                    if (value instanceof CarpetRule<?> carpetRule) rules.add(carpetRule);
                    else if (value != null) throw new IllegalStateException("RuleContext.rule() did not return a Carpet rule");
                }
                return rules;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                LOGGER.warn("Cannot read Igny rules through its public API", failure);
                return List.of();
            }
        }).withFancyName(fancyName)
            .withTranslations(language -> translations("carpet-igny-addition", instance, language));
    }

    private static Map<String, String> translations(String modId, Method instance, String language) {
        try {
            Object server = instance.invoke(null);
            if (server == null) return Map.of();
            if (!(server instanceof CarpetExtension extension)) {
                throw new IllegalStateException("Addon server does not implement CarpetExtension");
            }
            Map<String, String> result = extension.canHasTranslations(language);
            return result == null ? Map.of() : result;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            LOGGER.warn("Cannot read translations for {} through its public API", modId, failure);
            return Map.of();
        }
    }
}

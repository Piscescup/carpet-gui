package io.github.piscescup.fabricmc.carpetgui.api;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.SettingsManager;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Compatibility provider for installed addons that do not implement Carpet GUI's interfaces.
 * Manager IDs select a command root, not rule ownership. Shared managers require an explicit rule source.
 * Does not register rules, access Carpet internals, or create managers.
 */
public final class CarpetAddonAdapter implements CarpetModRulesApi {
    private final String modId;
    private final String managerId;
    private final String fancyName;
    private final Function<SettingsManager, ? extends Collection<? extends CarpetRule<?>>> ruleSource;
    private final Function<String, Map<String, String>> translations;

    private CarpetAddonAdapter(
        String modId, String managerId, String fancyName,
        Function<SettingsManager, ? extends Collection<? extends CarpetRule<?>>> ruleSource,
        Function<String, Map<String, String>> translations
    ) {
        this.modId = nonBlank(modId, "Mod ID");
        this.managerId = nonBlank(managerId, "Manager ID");
        this.fancyName = fancyName;
        this.ruleSource = Objects.requireNonNull(ruleSource, "Rule source");
        this.translations = translations;
    }

    /**
     * Selects every rule in a manager owned by this mod. Do not use this for a shared parent manager:
     * two IDs alone cannot distinguish an addon's rules from its parent's rules.
     */
    public static CarpetAddonAdapter fromIndependentManager(String modId, String managerId) {
        return new CarpetAddonAdapter(modId, managerId, null, SettingsManager::getCarpetRules, null);
    }

    /** Supplies the addon's live rule instances. Evaluated on discovery, not during construction. */
    public static CarpetAddonAdapter fromRules(
        String modId, String managerId, Supplier<? extends Collection<? extends CarpetRule<?>>> rules
    ) {
        Objects.requireNonNull(rules, "Rules supplier");
        return new CarpetAddonAdapter(modId, managerId, null, manager -> rules.get(), null);
    }

    /**
     * Resolves known addon rule names against the real manager. The caller is responsible for ownership;
     * a matching name alone cannot prove that a conflicting registration belonged to this addon.
     */
    public static CarpetAddonAdapter fromRuleNames(
        String modId, String managerId, Supplier<? extends Collection<String>> names
    ) {
        Objects.requireNonNull(names, "Rule names supplier");
        return new CarpetAddonAdapter(modId, managerId, null, manager -> {
            List<CarpetRule<?>> rules = new ArrayList<>();
            for (String name : Objects.requireNonNull(names.get(), "Addon rule names")) {
                if (name == null) continue;
                CarpetRule<?> rule = manager.getCarpetRule(name);
                if (rule != null) rules.add(rule);
            }
            return rules;
        }, null);
    }

    /**
     * Reads only declared field names and annotation metadata, never field values or private internals.
     * Use the addon's actual rule annotation; non-rule fields are excluded. Same ownership caveat as forRuleNames.
     */
    public static CarpetAddonAdapter fromSettingsClass(
        String modId, String managerId, Class<?> settingsClass, Class<? extends Annotation> ruleAnnotation
    ) {
        Objects.requireNonNull(settingsClass, "Settings class");
        Objects.requireNonNull(ruleAnnotation, "Rule annotation");
        List<String> names = new ArrayList<>();
        for (Field field : settingsClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(ruleAnnotation)) names.add(field.getName());
        }
        List<String> ruleNames = List.copyOf(names);
        return fromRuleNames(modId, managerId, () -> ruleNames);
    }

    /** Overrides the default Fabric metadata display name; returns a new provider. */
    public CarpetAddonAdapter withFancyName(String fancyName) {
        return new CarpetAddonAdapter(modId, managerId, nonBlank(fancyName, "Fancy name"), ruleSource, translations);
    }

    /** Overrides default runtime resource translations, e.g. extension::canHasTranslations. */
    public CarpetAddonAdapter withTranslations(Function<String, Map<String, String>> translations) {
        return new CarpetAddonAdapter(modId, managerId, fancyName, ruleSource,
            Objects.requireNonNull(translations, "Translations provider"));
    }

    @Override
    public String carpetModId() {
        return modId;
    }

    @Override
    public String carpetFancyName() {
        if (fancyName != null) return fancyName;
        return FabricLoader.getInstance().getModContainer(modId)
            .map(mod -> mod.getMetadata().getName()).orElse(modId);
    }

    /** The real command root used to locate this provider's manager, not its GUI page ID. */
    public String carpetManagerId() {
        return managerId;
    }

    @Override
    public Collection<CarpetRule<?>> getRules() {
        SettingsManager manager = findManager();
        if (manager == null) return List.of();
        Collection<? extends CarpetRule<?>> candidates = Objects.requireNonNull(ruleSource.apply(manager), "Addon rules");
        Map<String, CarpetRule<?>> rules = new LinkedHashMap<>();
        for (CarpetRule<?> rule : candidates) {
            // A failed/conflicting registration must not expose a detached rule as editable.
            if (rule != null && rule.settingsManager() == manager && manager.getCarpetRule(rule.name()) == rule) {
                rules.putIfAbsent(rule.name(), rule);
            }
        }
        return List.copyOf(rules.values());
    }

    @Override
    public Map<String, String> getTranslations(String language) {
        Map<String, String> result = translations == null
            ? CarpetModRulesApi.super.getTranslations(language) : translations.apply(language);
        return result == null ? Map.of() : Map.copyOf(result);
    }

    private SettingsManager findManager() {
        if (!FabricLoader.getInstance().isModLoaded(modId)) return null;
        SettingsManager base = CarpetServer.settingsManager;
        if (base != null && managerId.equals(base.identifier())) return base;
        for (CarpetExtension extension : List.copyOf(CarpetServer.extensions)) {
            SettingsManager manager = extension.extensionSettingsManager();
            if (manager != null && managerId.equals(manager.identifier())) return manager;
        }
        return null;
    }

    private static String nonBlank(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
        return value;
    }
}

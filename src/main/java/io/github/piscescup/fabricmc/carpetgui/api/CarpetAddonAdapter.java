package io.github.piscescup.fabricmc.carpetgui.api;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.SettingsManager;
import io.github.piscescup.fabricmc.carpetgui.integration.carpet.CarpetModLookup;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.piscescup.fabricmc.carpetgui.References.LOGGER;

/** Reflects an installed addon's extension singleton and Settings without importing addon types. */
public final class CarpetAddonAdapter implements CarpetModRulesApi {
    private final String modId;
    private final String metadataName;
    private final Class<? extends CarpetExtension> extensionClass;
    private final Method instanceGetter;
    private final Method listRules;
    private final List<String> ruleNames;
    private final Set<String> warnings = ConcurrentHashMap.newKeySet();

    /** Creates a provider; registration remains the caller's responsibility. */
    public static CarpetAddonAdapter fromClassNames(String extensionName, String settingsName)
        throws ReflectiveOperationException {
        return fromClassNames(extensionName, settingsName, null);
    }

    /** Optional exact annotation name; otherwise recognizes runtime annotations named Rule. */
    public static CarpetAddonAdapter fromClassNames(String extensionName, String settingsName, String annotationName)
        throws ReflectiveOperationException {
        if (extensionName == null || extensionName.isBlank() || settingsName == null || settingsName.isBlank()) {
            throw new IllegalArgumentException("Extension and Settings class names must not be blank");
        }
        ClassLoader loader = CarpetAddonAdapter.class.getClassLoader();
        Class<? extends CarpetExtension> extension = Class.forName(extensionName, false, loader).asSubclass(CarpetExtension.class);
        Class<?> settings = Class.forName(settingsName, false, loader);
        var mod = CarpetModLookup.find(extension, "")
            .orElseThrow(() -> new IllegalArgumentException("Cannot identify installed mod for " + extensionName));
        return new CarpetAddonAdapter(mod.getMetadata().getId(), mod.getMetadata().getName(), extension, settings, annotationName);
    }

    // Package-private construction also allows checks without a running Fabric game.
    CarpetAddonAdapter(String modId, String metadataName, Class<? extends CarpetExtension> extension,
        Class<?> settings, String annotationName) throws ReflectiveOperationException {
        this.modId = modId;
        this.metadataName = metadataName;
        extensionClass = extension;
        instanceGetter = extension.getMethod("getInstance");
        if (!Modifier.isStatic(instanceGetter.getModifiers())
            || !CarpetExtension.class.isAssignableFrom(instanceGetter.getReturnType())) {
            throw new IllegalArgumentException("getInstance() must be public static and return a CarpetExtension");
        }
        Method source;
        try {
            source = settings.getMethod("listRules");
            if (!Modifier.isStatic(source.getModifiers()) || !Collection.class.isAssignableFrom(source.getReturnType())) {
                throw new IllegalArgumentException("listRules() must be public static and return a Collection");
            }
        } catch (NoSuchMethodException absent) {
            source = null;
        }
        listRules = source;
        if (annotationName != null) {
            if (annotationName.isBlank()) throw new IllegalArgumentException("Empty annotation class name");
            Class.forName(annotationName, false, extension.getClassLoader()).asSubclass(Annotation.class);
        }
        List<String> names = new ArrayList<>();
        if (listRules == null) {
            for (var field : settings.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) continue;
                for (var annotation : field.getDeclaredAnnotations()) {
                    Class<?> type = annotation.annotationType();
                    if (annotationName == null ? type.getSimpleName().equals("Rule") : type.getName().equals(annotationName)) {
                        names.add(field.getName());
                        break;
                    }
                }
            }
        }
        ruleNames = List.copyOf(names);
    }

    @Override
    public String carpetModId() { return modId; }

    @Override
    public String carpetFancyName() {
        CarpetExtension extension = extension();
        return extension == null ? metadataName : CarpetModLookup.displayName(extension, metadataName);
    }

    /** Empty while the extension/manager is not ready; never invent a command-root ID. */
    public String carpetManagerId() {
        SettingsManager manager = manager(extension());
        return manager == null ? "" : manager.identifier();
    }

    @Override
    public Collection<CarpetRule<?>> getRules() {
        SettingsManager manager = manager(extension());
        if (manager == null) return List.of();
        try {
            List<CarpetRule<?>> candidates = new ArrayList<>();
            if (listRules == null) {
                for (String name : ruleNames) {
                    CarpetRule<?> rule = manager.getCarpetRule(name);
                    if (rule != null) candidates.add(rule);
                }
            } else {
                Object values = listRules.invoke(null);
                if (values == null) return List.of();
                for (Object value : (Collection<?>) values) {
                    if (value == null) continue;
                    Object candidate = value instanceof CarpetRule<?> ? value : value.getClass().getMethod("rule").invoke(value);
                    if (candidate == null) continue;
                    if (!(candidate instanceof CarpetRule<?> rule)) throw new IllegalArgumentException("Expected CarpetRule or wrapper.rule()");
                    candidates.add(rule);
                }
            }
            Map<String, CarpetRule<?>> registered = new LinkedHashMap<>();
            for (CarpetRule<?> rule : candidates) {
                if (rule.settingsManager() == manager && manager.getCarpetRule(rule.name()) == rule) {
                    registered.putIfAbsent(rule.name(), rule);
                }
            }
            return List.copyOf(registered.values());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            warn("rules", failure);
            return List.of();
        }
    }

    @Override
    public Map<String, String> getTranslations(String language) {
        try {
            CarpetExtension extension = extension();
            if (extension == null) return Map.of();
            Map<String, String> translations = extension.canHasTranslations(language);
            return translations == null ? Map.of() : Map.copyOf(translations);
        } catch (RuntimeException | LinkageError failure) {
            warn("translations", failure);
            return Map.of();
        }
    }

    private CarpetExtension extension() {
        try {
            Object value = instanceGetter.invoke(null);
            return value == null ? null : extensionClass.cast(value);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            warn("singleton", failure);
            return null;
        }
    }

    private SettingsManager manager(CarpetExtension extension) {
        if (extension == null) return null;
        try {
            SettingsManager manager = extension.extensionSettingsManager();
            return manager == null ? CarpetServer.settingsManager : manager;
        } catch (RuntimeException | LinkageError failure) {
            warn("manager", failure);
            return null;
        }
    }

    private void warn(String source, Throwable failure) {
        if (warnings.add(source)) LOGGER.warn("Cannot read {} for addon {}", source, modId, failure);
    }
}

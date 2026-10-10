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

package io.github.piscescup.fabricmc.carpetgui.adapter;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.SettingsManager;
import io.github.piscescup.fabricmc.carpetgui.api.CarpetModRulesApi;
import io.github.piscescup.fabricmc.carpetgui.resources.CarpetTranslationResources;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.piscescup.fabricmc.carpetgui.References.FABRIC_LOADER;
import static io.github.piscescup.fabricmc.carpetgui.References.LOGGER;

/**
 * Reflects an installed addon's extension and settings without importing addon types.
 */
public abstract class CarpetAddonAdapter
    implements CarpetModRulesApi
{
    protected final String modId;
    protected final String parentPackage;
    protected PackageRef extensionClassRef;
    protected PackageRef settingsClassRef;

    protected String carpetExtensionClassCanonicalName;
    protected String carpetSettingsClassCanonicalName;

    protected Accessor<?> settingsAccessor;
    protected Accessor<?> extensionAccessor;

    protected final Set<String> warnings = ConcurrentHashMap.newKeySet();

    protected volatile CarpetExtension extension;
    protected volatile SettingsManager manager;

    /** Identity-only constructor for adapters that resolve a manager directly. */
    protected CarpetAddonAdapter(
        Builder<?> builder
    ) {
        this.modId = builder.modId;
        this.parentPackage = builder.packageName;
        this.extensionClassRef = builder.extensionClassRef;
        this.settingsClassRef = builder.settingsClassRef;
        this.carpetExtensionClassCanonicalName = builder.carpetExtensionClassCanonicalName;
        this.carpetSettingsClassCanonicalName = builder.carpetSettingsClassCanonicalName;
        this.settingsAccessor = builder.settingsAccessor;
        this.extensionAccessor = builder.extensionAccessor;
    }

    public String parentPackage() { return parentPackage; }
    public PackageRef extensionClassRef() { return extensionClassRef; }
    public PackageRef settingsClassRef() { return settingsClassRef; }
    public Accessor<?> extensionAccessor() { return extensionAccessor; }
    public Accessor<?> settingsAccessor() { return settingsAccessor; }

    @Override
    public String carpetModId() {
        return modId;
    }

    @Override
    public String carpetFancyName() {
        return FABRIC_LOADER.getModContainer(modId)
            .orElseThrow()
            .getMetadata()
            .getName();
    }

    @Override
    public Set<String> getSettingsManagerIds() {
        SettingsManager value = settingsManager();
        return value == null || value == CarpetServer.settingsManager ?
            Set.of() :
            Set.of(value.identifier());
    }

    /**
     * Empty while the extension or manager is not ready.
     */
    public String carpetManagerId() {
        SettingsManager value = settingsManager();
        return value == null ? "" : value.identifier();
    }

    /**
     * Resolves the reflected names to the same live rule instances registered in the real manager.
     */
    public final Collection<CarpetRule<?>> asCarpetRules() {
        SettingsManager value = settingsManager();
        if (value == null) return List.of();
        try {
            Collection<String> names = resolveRuleNames(value);
            if (names == null) return List.of();
            Map<String, CarpetRule<?>> resolved = new LinkedHashMap<>();
            for (String name : names) {
                if (name == null || name.isBlank()) continue;
                CarpetRule<?> rule = value.getCarpetRule(name);
                if (rule != null && rule.settingsManager() == value) resolved.putIfAbsent(rule.name(), rule);
            }
            return List.copyOf(resolved.values());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            warn("rules", failure);
            return List.of();
        }
    }

    /** Manager context is separate from arguments passed to reflected rule-name methods. */
    protected Collection<String> resolveRuleNames(SettingsManager manager)
        throws ReflectiveOperationException
    {
        return getRuleNames();
    }

    /**
     * Returns rule names discovered from the addon's own settings representation.
     */
    protected abstract Collection<String> getRuleNames(Object... args)
        throws InvocationTargetException, IllegalAccessException, ClassNotFoundException, NoSuchFieldException,
               NoSuchMethodException, InstantiationException;

    @Override
    public Map<String, String> getTranslations(String language) {
        try {
            CarpetExtension value = extension();
            if (value == null) return CarpetTranslationResources.read(carpetModId(), language);
            Map<String, String> translations = value.canHasTranslations(language);
            return translations == null ? Map.of() : Map.copyOf(translations);
        } catch (RuntimeException | LinkageError failure) {
            warn("translations", failure);
            return Map.of();
        }
    }

    protected Class<?> getSettingsClass() throws ClassNotFoundException {
        return load(this.carpetSettingsClassCanonicalName);
    }

    /**
     * Reads a settings object without constructing a new instance.
     * Static access uses the configured settings class; instance access uses the live extension.
     * Null means that the extension or settings object is not ready and will be retried later.
     */
    protected Object getSettingsInstance() throws ClassNotFoundException {
        Accessor<?> accessor = Objects.requireNonNull(settingsAccessor, "Settings accessor");
        Class<?> settingsClass = getSettingsClass();
        Object target = accessor.isStaticAccess() ? settingsClass : extension();
        if (target == null) return null;
        return settingsClass.cast(accessor.get(target));
    }

    protected CarpetExtension extension() {
        if (this.carpetExtensionClassCanonicalName == null) return null;
        CarpetExtension current = extension;

        if (current != null) return current;

        try {
            Class<? extends CarpetExtension> extensionClass =
                load(this.carpetExtensionClassCanonicalName)
                    .asSubclass(CarpetExtension.class);

            Object target = this.extensionAccessor.isStaticAccess()
                ? extensionClass
                : extensionClass.getDeclaredConstructor().newInstance();

            Object value = this.extensionAccessor.get(target);
            if (value != null) {
                extension = current = extensionClass.cast(value);
            }

            return current;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            warn("extension", failure);
            return null;
        }
    }

    /** Override to resolve a manager without a Carpet extension. */
    protected SettingsManager settingsManager() {
        return manager(extension());
    }

    protected SettingsManager manager(CarpetExtension extension) {
        SettingsManager current = manager;
        if (current != null || extension == null) return current;
        try {
            current = extension.extensionSettingsManager();
            if (current == null) current = CarpetServer.settingsManager;
            if (current != null) manager = current;
            return current;
        } catch (RuntimeException | LinkageError failure) {
            warn("settings manager", failure);
            return null;
        }
    }

    protected void warn(String source, Throwable failure) {
        if (warnings.add(source)) LOGGER.warn("Cannot read {} for addon {}", source, modId, failure);
    }

    protected static Class<?> load(String classCanonicalName) throws ClassNotFoundException {
        return Class.forName(classCanonicalName, false, CarpetAddonAdapter.class.getClassLoader());
    }

    public static RuleAdapterBuilder ruleBuilder(String modId, String parentPackage) {
        return new RuleBuilder(modId, parentPackage);
    }

    public static ManagerAdapterBuilder managerBuilder(String modId, String parentPackage) {
        return new ManagerBuilder(modId, parentPackage);
    }

    public static AnnotationAdapterBuilder annotationBuilder(String modId, String parentPackage) {
        return new AnnotationBuilder(modId, parentPackage);
    }
}

abstract class Builder<AB extends AdapterBuilder<AB>>
    implements AdapterBuilder<AB>
{
    protected final String modId;
    protected String packageName;
    protected PackageRef extensionClassRef;
    protected PackageRef settingsClassRef;

    protected String carpetExtensionClassCanonicalName;
    protected String carpetSettingsClassCanonicalName;

    protected Accessor<?> settingsAccessor = Accessor.ofStaticField("SETTINGS");

    protected Accessor<?> extensionAccessor = Accessor.ofStaticField("INSTANCE");

    protected Builder(String modId, String packageName) {
        this.modId = modId;
        this.packageName = packageName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public AB carpetExtensionClassName(PackageRef packageRef) {
        this.extensionClassRef = Objects.requireNonNull(packageRef, "Extension class");
        this.carpetExtensionClassCanonicalName = packageRef.toCanonicalPackage(this.packageName);
        return (AB) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public AB settingsClassName(PackageRef packageRef) {
        this.settingsClassRef = Objects.requireNonNull(packageRef, "Settings class");
        this.carpetSettingsClassCanonicalName =
            packageRef.toCanonicalPackage(this.packageName);
        return (AB) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public AB extensionAccessor(Accessor<?> extensionAccessor) {
        this.extensionAccessor = extensionAccessor;
        return (AB) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public AB settingsAccessor(Accessor<?> settingsAccessor) {
        this.settingsAccessor = settingsAccessor;
        return (AB) this;
    }
}

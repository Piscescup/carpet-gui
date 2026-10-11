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
import io.github.piscescup.fabricmc.carpetgui.util.CarpetTranslationUtils;

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
 * Adapts an installed Carpet addon to {@link CarpetModRulesApi} through deferred reflection,
 * without adding a compile-time dependency on the addon's classes. Subclasses differ only in how
 * they discover rule names or obtain a dedicated settings manager. Missing classes, unavailable
 * runtime instances, and reflection failures are logged once per source and exposed as empty data.
 *
 * <pre>{@code
 * CarpetAddonAdapter adapter = CarpetAddonAdapter.annotationBuilder("my_mod", "dev.example")
 *     .settingsClassName("MySettings")
 *     .onCarpetRuleAnnotation()
 *     .build();
 * Collection<CarpetRule<?>> rules = adapter.asCarpetRules();
 * }</pre>
 *
 * @see RuleClassCarpetAddonAdapter
 * @see AnnotationCarpetAddonAdapter
 * @see ManagerCarpetAddonAdapter
 * @see CarpetModRulesApi
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

    /**
     * Copies the validated builder configuration into a new adapter.
     *
     * @param builder the concrete builder containing class references and accessors
     */
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

    /**
     * Returns the package used to resolve relative class references.
     *
     * @return the configured parent package, possibly {@code null} when all references are absolute
     */
    public String parentPackage() { return parentPackage; }

    /**
     * Returns the unresolved extension class reference.
     *
     * @return the configured reference, or {@code null} when no extension is required
     */
    public PackageRef extensionClassRef() { return extensionClassRef; }

    /**
     * Returns the unresolved settings class reference.
     *
     * @return the configured reference, or {@code null} for manager-only adapters
     */
    public PackageRef settingsClassRef() { return settingsClassRef; }

    /**
     * Returns the configured extension accessor.
     *
     * @return the accessor used to obtain a live extension, possibly {@code null}
     */
    public Accessor<?> extensionAccessor() { return extensionAccessor; }

    /**
     * Returns the configured settings-instance accessor.
     *
     * @return the accessor used to obtain settings, possibly {@code null}
     */
    public Accessor<?> settingsAccessor() { return settingsAccessor; }

    /**
     * Returns the Fabric mod identifier supplied to the builder.
     *
     * @return the configured mod ID
     */
    @Override
    public String carpetModId() {
        return modId;
    }

    /**
     * Looks up the addon's display name in Fabric Loader metadata.
     *
     * <pre>{@code
     * String tabTitle = adapter.carpetFancyName();
     * }</pre>
     *
     * @return the installed mod's metadata name
     * @throws java.util.NoSuchElementException if no loaded mod has the configured ID
     */
    @Override
    public String carpetFancyName() {
        return FABRIC_LOADER.getModContainer(modId)
            .orElseThrow()
            .getMetadata()
            .getName();
    }

    /**
     * Returns the identifier of the dedicated manager owned by this adapter. Carpet's global
     * manager is deliberately not claimed.
     *
     * @return a singleton manager-ID set, or an empty set while unavailable or when using the global manager
     */
    @Override
    public Set<String> getSettingsManagerIds() {
        SettingsManager value = settingsManager();
        return value == null || value == CarpetServer.settingsManager ?
            Set.of() :
            Set.of(value.identifier());
    }

    /**
     * Returns the current settings manager identifier.
     *
     * <pre>{@code
     * String commandRoot = adapter.carpetManagerId();
     * }</pre>
     *
     * @return the manager ID, or an empty string while the extension or manager is not ready
     */
    public String carpetManagerId() {
        SettingsManager value = settingsManager();
        return value == null ? "" : value.identifier();
    }


    /**
     * Resolves names discovered by {@link #getRuleNames(Object...)} against the live manager.
     * Missing names are skipped and duplicate rules are removed in discovery order. Reflection,
     * runtime, and linkage failures are logged once per source and exposed as an empty collection.
     *
     * <pre>{@code
     * Collection<CarpetRule<?>> rules = adapter.asCarpetRules();
     * }</pre>
     *
     * @return an immutable collection of registered rules, or an empty collection while unavailable
     * @see CarpetModRulesApi#asCarpetRules()
     */
    @Override
    public Collection<CarpetRule<?>> asCarpetRules() {
        try {
            SettingsManager value = settingsManager();
            if (value == null) return List.of();
            Collection<String> names = getRuleNames();
            if (names == null) return List.of();
            Map<String, CarpetRule<?>> resolved = new LinkedHashMap<>();
            for (String name : names) {
                if (name == null || name.isBlank()) continue;
                CarpetRule<?> rule = value.getCarpetRule(name);
                if (rule != null && rule.settingsManager() == value) {
                    resolved.putIfAbsent(rule.name(), rule);
                }
            }
            return List.copyOf(resolved.values());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            warn("rules", failure);
            return List.of();
        }
    }

    /**
     * Returns rule names discovered from the addon's own settings representation.
     *
     * @param args optional subclass-specific invocation arguments
     * @return the discovered rule names, or an empty collection when backing data is not ready
     * @throws InvocationTargetException if a reflected addon method throws
     * @throws IllegalAccessException if a reflected member cannot be accessed
     * @throws ClassNotFoundException if a configured addon class is unavailable
     * @throws NoSuchFieldException if a configured field is unavailable
     * @throws NoSuchMethodException if a configured method or constructor is unavailable
     * @throws InstantiationException if a configured class cannot be instantiated
     */
    protected abstract Collection<String> getRuleNames(Object... args)
        throws InvocationTargetException, IllegalAccessException, ClassNotFoundException, NoSuchFieldException,
               NoSuchMethodException, InstantiationException;

    /**
     * Reads translations from the live extension when available, otherwise from the mod's language
     * resources. Runtime and linkage failures are logged and converted to an empty map.
     *
     * @param language the Minecraft language code, such as {@code "en_us"}
     * @return an immutable translation map, a resource-backed map, or an empty map on failure
     */
    @Override
    public Map<String, String> getTranslations(String language) {
        try {
            CarpetExtension value = extension();
            if (value == null) return CarpetTranslationUtils.read(carpetModId(), language);
            Map<String, String> translations = value.canHasTranslations(language);
            return translations == null ? Map.of() : Map.copyOf(translations);
        } catch (RuntimeException | LinkageError failure) {
            warn("translations", failure);
            return Map.of();
        }
    }

    /**
     * Loads the configured settings class without initializing it.
     *
     * @return the configured settings class
     * @throws ClassNotFoundException if the configured class cannot be loaded
     */
    protected Class<?> getSettingsClass() throws ClassNotFoundException {
        return load(this.carpetSettingsClassCanonicalName);
    }

    /**
     * Reads a settings object without constructing a new instance.
     * Static access uses the configured settings class; instance access uses the live extension.
     * Null means that the extension or settings object is not ready and will be retried later.
     *
     * @return the settings instance, or {@code null} while its backing object is unavailable
     * @throws ClassNotFoundException if the configured settings class cannot be loaded
     * @throws NullPointerException if no settings accessor was configured
     * @throws IllegalArgumentException if the accessor result is not an instance of the settings class
     * @throws IllegalStateException if the configured member cannot be read
     */
    protected Object getSettingsInstance() throws ClassNotFoundException {
        Accessor<?> accessor = Objects.requireNonNull(settingsAccessor, "Settings accessor");
        Class<?> settingsClass = getSettingsClass();
        Object target = accessor.isStaticAccess() ? settingsClass : extension();
        if (target == null) return null;
        return settingsClass.cast(accessor.get(target));
    }

    /**
     * Lazily resolves and caches the live Carpet extension. Failures are logged and retried on a
     * later call.
     *
     * @return the live extension, or {@code null} when none is configured or currently available
     */
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

    /**
     * Resolves the settings manager for this addon. When no extension is configured, uses Carpet's
     * global manager; a configured but unavailable extension is retried later. Subclasses may
     * override this to read a dedicated manager without a Carpet extension.
     *
     * @return the live manager, or {@code null} while unavailable
     */
    public SettingsManager settingsManager() {
        if (carpetExtensionClassCanonicalName == null) return CarpetServer.settingsManager;
        return manager(extension());
    }

    /**
     * Reads the extension's current manager, falling back to Carpet's global manager when the live
     * extension returns null. Neither result is cached, so later initialization or manager
     * replacement is visible on the next call.
     *
     * @param extension the live extension, or {@code null} when it is not ready
     * @return the resolved manager, or {@code null} when unavailable
     */
    protected SettingsManager manager(CarpetExtension extension) {
        if (extension == null) return null;
        try {
            SettingsManager current = extension.extensionSettingsManager();
            return current == null ? CarpetServer.settingsManager : current;
        } catch (RuntimeException | LinkageError failure) {
            warn("settings manager", failure);
            return null;
        }
    }

    /**
     * Logs the first failure for a logical source and suppresses repeated messages for that source.
     *
     * @param source short label identifying the failed lookup
     * @param failure the failure to include in the diagnostic log
     */
    protected void warn(String source, Throwable failure) {
        if (warnings.add(source)) LOGGER.warn("Cannot read {} for addon {}", source, modId, failure);
    }

    /**
     * Loads a class through Carpet GUI's class loader without initializing it.
     *
     * @param classCanonicalName the fully qualified binary-compatible class name
     * @return the loaded class
     * @throws ClassNotFoundException if the named class cannot be found
     * @throws LinkageError if the class or one of its dependencies cannot be linked
     */
    protected static Class<?> load(String classCanonicalName) throws ClassNotFoundException {
        return Class.forName(classCanonicalName, false, CarpetAddonAdapter.class.getClassLoader());
    }

    /**
     * Starts a collection-based rule adapter configuration.
     *
     * <pre>{@code
     * RuleAdapterBuilder builder = CarpetAddonAdapter.ruleBuilder("my_mod", "dev.example");
     * }</pre>
     *
     * @param modId the Fabric mod identifier
     * @param parentPackage package used for relative class references; may be {@code null} when unused
     * @return a new rule-class adapter builder
     */
    public static RuleAdapterBuilder ruleBuilder(String modId, String parentPackage) {
        return new RuleBuilder(modId, parentPackage);
    }

    /**
     * Starts a dedicated-manager adapter configuration.
     *
     * <pre>{@code
     * ManagerAdapterBuilder builder = CarpetAddonAdapter.managerBuilder("my_mod", "dev.example");
     * }</pre>
     *
     * @param modId the Fabric mod identifier
     * @param parentPackage package used for relative class references; may be {@code null} when unused
     * @return a new manager adapter builder
     */
    public static ManagerAdapterBuilder managerBuilder(String modId, String parentPackage) {
        return new ManagerBuilder(modId, parentPackage);
    }

    /**
     * Starts an annotation-based rule adapter configuration.
     *
     * <pre>{@code
     * AnnotationAdapterBuilder builder = CarpetAddonAdapter.annotationBuilder("my_mod", "dev.example");
     * }</pre>
     *
     * @param modId the Fabric mod identifier
     * @param parentPackage package used for relative class references; may be {@code null} when unused
     * @return a new annotation adapter builder
     */
    public static AnnotationAdapterBuilder annotationBuilder(String modId, String parentPackage) {
        return new AnnotationBuilder(modId, parentPackage);
    }
}

/**
 * Shared mutable implementation behind the public fluent builder interfaces.
 *
 * @param <AB> the public builder interface returned from fluent calls
 */
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

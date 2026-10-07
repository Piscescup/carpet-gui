package io.github.piscescup.fabricmc.carpetgui.adapter;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;
import carpet.api.settings.SettingsManager;
import io.github.piscescup.fabricmc.carpetgui.api.CarpetModRulesApi;
import io.github.piscescup.fabricmc.carpetgui.integration.carpet.CarpetModLookup;
import org.lwjgl.system.ffm.FFMReturn;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static io.github.piscescup.fabricmc.carpetgui.References.LOGGER;

/**
 * Reflects an installed addon's extension and settings without importing addon types.
 */
public abstract class CarpetAddonAdapter
    implements CarpetModRulesApi
{
    public static final String DOT = ".";

    private final String modId;
    private final String modFancyName;
    protected String extensionClassName;
    protected String extensionFieldName;
    protected String settingsClassName;
    private final Set<String> warnings = ConcurrentHashMap.newKeySet();

    protected volatile CarpetExtension extension;
    protected volatile SettingsManager manager;
    protected String packageName;

    CarpetAddonAdapter(
        String modId,
        String modFancyName,
        String extensionClassName,
        String extensionFieldName,
        String settingsClassName
    ) {
        this.modId = requireText(modId, "Mod ID");
        this.modFancyName = requireText(modFancyName, "Mod fancy name");
        this.extensionClassName = requireText(extensionClassName, "Extension class");
        this.extensionFieldName = extensionFieldName;
        this.settingsClassName = requireText(settingsClassName, "Settings class");
    }

    @Override
    public String carpetModId() {
        return modId;
    }

    @Override
    public String carpetFancyName() {
        CarpetExtension value = extension();
        return value == null ? modFancyName : CarpetModLookup.displayName(value, modFancyName);
    }

    /**
     * Empty while the extension or manager is not ready.
     */
    public String carpetManagerId() {
        SettingsManager value = manager(extension());
        return value == null ? "" : value.identifier();
    }

    /**
     * Resolves the reflected names to the same live rule instances registered in the real manager.
     */
    public final Collection<CarpetRule<?>> asCarpetRules() {
        SettingsManager value = manager(extension());
        if (value == null) return List.of();
        try {
            Collection<String> names = getRuleNames();
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
            if (value == null) return Map.of();
            Map<String, String> translations = value.canHasTranslations(language);
            return translations == null ? Map.of() : Map.copyOf(translations);
        } catch (RuntimeException | LinkageError failure) {
            warn("translations", failure);
            return Map.of();
        }
    }

    protected Class<?> getSettingsClass() throws ClassNotFoundException {
        return load(settingsClassName);
    }

    protected CarpetExtension extension() {
        CarpetExtension current = extension;
        if (current != null) return current;
        try {
            Class<? extends CarpetExtension> extensionClass = load(extensionClassName)
                .asSubclass(CarpetExtension.class);

            if (extensionFieldName == null) {
                current = extensionClass.getDeclaredConstructor()
                    .newInstance();
            } else {
                Field field = extensionClass.getDeclaredField(extensionFieldName);
                if (!Modifier.isStatic(field.getModifiers())) {
                    throw new IllegalArgumentException(extensionFieldName + " must be a static extension field");
                }
                field.setAccessible(true);
                Object value = field.get(null);
                if (value != null) extension = current = extensionClass.cast(value);
            }

            return current;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            warn("extension", failure);
            return null;
        }
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

    public CarpetAddonAdapter withPackage(String packageName) {
        this.packageName = packageName;
        return this;
    }

    protected void warn(String source, Throwable failure) {
        if (warnings.add(source)) LOGGER.warn("Cannot read {} for addon {}", source, modId, failure);
    }

    protected Class<?> load(String className) throws ClassNotFoundException {
        return Class.forName(this.packageName + DOT + className, false, CarpetAddonAdapter.class.getClassLoader());
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
        return value;
    }

    public static <T> RuleClassCarpetAddonAdapter<T> fromMethod(
        String modId,
        String modFancyName,
        String extensionClassName,
        String extensionFieldName,
        String settingsClassName,
        String listSettingsFieldName,
        String ruleClassClassName,
        String stringMethodName,
        Function<T, String> mapper,
        Class<?>... parameterTypes
    ) {
        return new RuleClassCarpetAddonAdapter<>(
            modId,
            modFancyName,
            extensionClassName,
            extensionFieldName,
            settingsClassName,
            listSettingsFieldName,
            ruleClassClassName,
            stringMethodName,
            Objects.requireNonNull(mapper, "mapper"),
            parameterTypes
        );
    }

    public static <T> RuleClassCarpetAddonAdapter<T> fromMethod(
        String modId,
        String modFancyName,
        String extensionClassName,
        String settingsClassName,
        String ruleClassClassName,
        String stringMethodName,
        Function<T, String> mapper,
        Class<?>... parameterTypes
    ) {
        return fromMethod(
            modId, modFancyName, extensionClassName, "INSTANCE", settingsClassName, "RULES",
            ruleClassClassName, stringMethodName, mapper, parameterTypes
        );
    }

    public static <T> RuleClassCarpetAddonAdapter<T> fromMethod(
        String modId,
        String modFancyName,
        String extensionClassName,
        String settingsClassName,
        String ruleClassClassName,
        String stringMethodName,
        Class<?>... parameterTypes
    ) {
        return fromMethod(
            modId, modFancyName, extensionClassName, settingsClassName, ruleClassClassName,
            stringMethodName, String::valueOf, parameterTypes
        );
    }

    public static AnnotationCarpetAddonAdapter fromAnnotation(
        String modId,
        String modFancyName,
        String extensionClassName,
        String extensionFieldName,
        String settingsClassName,
        String annotationClassName
    ) {
        return new AnnotationCarpetAddonAdapter(
            modId, modFancyName, extensionClassName, extensionFieldName, settingsClassName,
            annotationClassName
        );
    }

    public static AnnotationCarpetAddonAdapter fromAnnotationOnCarpetRule(
        String modId,
        String modFancyName,
        String extensionClassName,
        String extensionFieldName,
        String settingsClassName
    ) {
        return new AnnotationCarpetAddonAdapter(modId, modFancyName, extensionClassName, extensionFieldName, settingsClassName, Rule.class);
    }

    public static AnnotationCarpetAddonAdapter fromAnnotation(
        String modId,
        String modFancyName,
        String extensionClassName,
        String settingsClassName,
        String annotationClassName
    ) {
        return fromAnnotation(
            modId, modFancyName, extensionClassName, "INSTANCE", settingsClassName, annotationClassName
        );
    }

    public static AnnotationCarpetAddonAdapter fromAnnotation(
        String modId,
        String modFancyName,
        String extensionClassName,
        String settingsClassName
    ) {
        return new AnnotationCarpetAddonAdapter(
            modId, modFancyName, extensionClassName, "INSTANCE", settingsClassName, Rule.class
        );
    }

    public static CarpetAddonAdapter rof() {
        return new CarpetAddonAdapter(
            "carpet-rof-addition", "Carpet ROF Addition",
            "ROFCarpetServer", null, "ROFSettings"
        ) {
            @Override
            protected Collection<String> getRuleNames(Object... args)
                throws IllegalAccessException, ClassNotFoundException, NoSuchFieldException
            {
                Class<?> settingsIndex = load(this.settingsClassName);
                Field ruleClassesField = settingsIndex.getDeclaredField("ruleClasses");
                ruleClassesField.setAccessible(true);
                Object value = ruleClassesField.get(null);
                if (!(value instanceof Collection<?> ruleClasses)) {
                    throw new IllegalStateException("ROFSettings.ruleClasses must be a Collection");
                }

                return ruleClasses
                    .stream()
                    .map(candidate -> {
                        if (!(candidate instanceof Class<?> ruleClass)) {
                            throw new IllegalStateException("ROFSettings.ruleClasses contains a non-Class value");
                        }
                        return ruleClass;
                    })
                    .flatMap(ruleClass -> java.util.Arrays.stream(ruleClass.getDeclaredFields()))
                    .filter(field -> field.isAnnotationPresent(Rule.class))
                    .map(Field::getName)
                    .distinct()
                    .toList();
            }
        };
    }
}

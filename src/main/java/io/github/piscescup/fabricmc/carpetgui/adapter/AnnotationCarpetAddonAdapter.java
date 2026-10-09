package io.github.piscescup.fabricmc.carpetgui.adapter;


import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Discovers rule names from annotated static settings fields. */
public class AnnotationCarpetAddonAdapter extends CarpetAddonAdapter {
    private final PackageRef annotationClassRef;
    private final String annotationClassCanonicalName;
    private final Accessor<?> settingsClassesAccessor;

    AnnotationCarpetAddonAdapter(
        AnnotationBuilder builder
    ) {
        super(builder);
        this.annotationClassRef = builder.annotationClassRef;
        this.annotationClassCanonicalName = builder.annotationClassCanonicalName;
        this.settingsClassesAccessor = builder.settingsClassesAccessor;
    }

    public PackageRef annotationClassRef() { return annotationClassRef; }
    public Accessor<?> settingsClassesAccessor() { return settingsClassesAccessor; }

    @Override
    protected Collection<String> getRuleNames(Object... args) throws ClassNotFoundException {
        Collection<String> names = new ArrayList<>();
        Class<? extends Annotation> ruleAnnotation = getAnnotation();

        Class<?>[] classes;
        if (settingsClassesAccessor == null) {
            Class<?> settingsClass = getSettingsClass();
            Class<?>[] innerClasses = settingsClass.getDeclaredClasses();
            classes = Arrays.copyOf(innerClasses, innerClasses.length + 1);
            classes[innerClasses.length] = settingsClass;
        } else {
            Object target = settingsClassesAccessor.isStaticAccess() ? getSettingsClass() : getSettingsInstance();
            if (target == null) return List.of();
            Object value = settingsClassesAccessor.get(target);
            if (value == null) return List.of();
            if (!(value instanceof Collection<?> indexed)) {
                throw new IllegalArgumentException("Settings class index for " + modId + " must be a Collection");
            }
            classes = new Class<?>[indexed.size()];
            int index = 0;
            for (Object candidate : indexed) {
                if (!(candidate instanceof Class<?> type)) {
                    throw new IllegalArgumentException("Settings class index for " + modId + " contains a non-Class value");
                }
                classes[index++] = type;
            }
        }

        Arrays.stream(classes)
            .flatMap(clazz -> Arrays.stream(clazz.getDeclaredFields()))
            .filter(field -> field.isAnnotationPresent(ruleAnnotation))
            .map(Field::getName)
            .forEach(names::add);

        return names;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Annotation> getAnnotation() throws ClassNotFoundException {

        Class<?> candidate = load(annotationClassCanonicalName);
        if (!candidate.isAnnotation()) {
            throw new IllegalArgumentException(annotationClassCanonicalName + " is not an annotation");
        }
        return (Class<? extends Annotation>) candidate;
    }
}

class AnnotationBuilder
    extends Builder<AnnotationAdapterBuilder>
    implements AnnotationAdapterBuilder
{
    PackageRef annotationClassRef;
    String annotationClassCanonicalName;
    Accessor<?> settingsClassesAccessor;

    protected AnnotationBuilder(String modId, String packageName) {
        super(modId, packageName);
    }

    @Override
    public AnnotationAdapterBuilder ruleAnnotationClassName(PackageRef packageRef) {
        this.annotationClassRef = Objects.requireNonNull(packageRef, "Class reference");
        this.annotationClassCanonicalName =
            packageRef.toCanonicalPackage(this.packageName);
        return this;
    }

    @Override
    public AnnotationAdapterBuilder settingsClassesAccessor(Accessor<?> accessor) {
        this.settingsClassesAccessor = Objects.requireNonNull(accessor, "Settings classes accessor");
        return this;
    }

    @Override
    public AnnotationCarpetAddonAdapter build() {
        requireText(modId, "Mod ID");
        requireText(carpetSettingsClassCanonicalName, "Settings class");
        requireText(annotationClassCanonicalName, "Rule annotation class");
        if (settingsClassesAccessor != null && !settingsClassesAccessor.isStaticAccess()) {
            Objects.requireNonNull(settingsAccessor, "Settings accessor");
        }
        if (carpetExtensionClassCanonicalName != null) {
            Objects.requireNonNull(extensionAccessor, "Extension accessor");
        }
        return new AnnotationCarpetAddonAdapter(this);
    }
    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
    }
}

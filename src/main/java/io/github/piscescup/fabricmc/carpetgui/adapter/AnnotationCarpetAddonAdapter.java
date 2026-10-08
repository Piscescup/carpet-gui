package io.github.piscescup.fabricmc.carpetgui.adapter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/** Discovers rule names from annotated static settings fields. */
public class AnnotationCarpetAddonAdapter extends CarpetAddonAdapter {
    private volatile Class<? extends Annotation> annotation;
    private final String annotationClassName;

    AnnotationCarpetAddonAdapter(
        String modId,
        String modFancyName,
        String extensionClassName,
        String extensionFieldName,
        String settingsClassName,
        Class<? extends Annotation> annotation
    ) {
        super(modId, modFancyName, extensionClassName, extensionFieldName, settingsClassName);
        if (annotation == null) {
            throw new IllegalArgumentException("Rule annotation class must not be null");
        }
        this.annotation = annotation;
        this.annotationClassName = null;
    }

    AnnotationCarpetAddonAdapter(
        String modId,
        String modFancyName,
        String extensionClassName,
        String extensionFieldName,
        String settingsClassName,
        String annotationClassName
    ) {
        super(modId, modFancyName, extensionClassName, extensionFieldName, settingsClassName);
        if (annotationClassName == null || annotationClassName.isBlank()) {
            throw new IllegalArgumentException("Rule annotation class name must not be blank");
        }
        this.annotationClassName = annotationClassName;
    }

    @Override
    protected Collection<String> getRuleNames(Object... args) throws ClassNotFoundException {
        Collection<String> names = new ArrayList<>();
        Class<? extends Annotation> ruleAnnotation = getAnnotation();

        Class<?>[] innerClasses = getSettingsClass().getDeclaredClasses();

        Class<?>[] classes = Arrays.copyOf(innerClasses, innerClasses.length + 1);
        classes[innerClasses.length] = getSettingsClass();

        Arrays.stream(classes)
            .flatMap(clazz -> Arrays.stream(clazz.getDeclaredFields()))
            .filter(field -> field.isAnnotationPresent(ruleAnnotation))
            .map(Field::getName)
            .forEach(names::add);

        return names;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Annotation> getAnnotation() throws ClassNotFoundException {
        Class<? extends Annotation> current = annotation;
        if (current != null) return current;

        Class<?> candidate = Class.forName(
            this.packageName + DOT + annotationClassName,
            false,
            CarpetAddonAdapter.class.getClassLoader()
        );
        if (!candidate.isAnnotation()) {
            throw new IllegalArgumentException(annotationClassName + " is not an annotation");
        }
        annotation = current = (Class<? extends Annotation>) candidate;
        return current;
    }
}

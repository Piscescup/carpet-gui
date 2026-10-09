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

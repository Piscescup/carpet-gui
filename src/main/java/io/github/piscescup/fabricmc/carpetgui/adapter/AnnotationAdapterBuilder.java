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

import carpet.api.settings.Rule;

/**
 * Builds an {@link AnnotationCarpetAddonAdapter} that discovers rule names from annotated static
 * settings fields.
 *
 * <pre>{@code
 * CarpetAddonAdapter adapter = CarpetAddonAdapter.annotationBuilder("my_mod", "dev.example")
 *     .settingsClassName("MySettings")
 *     .onCarpetRuleAnnotation()
 *     .build();
 * }</pre>
 *
 * @author REN YuanTong
 * @since 1.0.0
 * @see AnnotationCarpetAddonAdapter
 * @see CarpetAddonAdapter#annotationBuilder(String, String)
 */
public interface AnnotationAdapterBuilder extends AdapterBuilder<AnnotationAdapterBuilder> {
    /**
     * Configures the annotation used to mark rule fields.
     *
     * @param packageRef absolute or parent-relative annotation class reference
     * @return this builder
     * @throws NullPointerException if {@code packageRef} is {@code null}
     * @throws IllegalArgumentException if a relative reference has no parent package
     */
    AnnotationAdapterBuilder ruleAnnotationClassName(PackageRef packageRef);

    /**
     * Configures an optional index of annotated settings classes, such as
     * {@code ROFSettings.ruleClasses}. Without this accessor, the adapter scans the settings class
     * and its directly declared nested classes.
     *
     * <pre>{@code
     * builder.settingsClassesAccessor(Accessor.ofStaticField("ruleClasses"));
     * }</pre>
     *
     * @param accessor accessor whose value must be a collection of {@link Class} objects
     * @return this builder
     * @throws NullPointerException if {@code accessor} is {@code null}
     */
    AnnotationAdapterBuilder settingsClassesAccessor(Accessor<?> accessor);

    /**
     * Configures a parent-relative rule annotation class name.
     *
     * @param ruleAnnotationClassName class name relative to the builder's parent package
     * @return this builder
     * @throws IllegalArgumentException if the name is blank or no parent package is available
     */
    default AnnotationAdapterBuilder ruleAnnotationClassName(String ruleAnnotationClassName) {
        return ruleAnnotationClassName(PackageRef.relative(ruleAnnotationClassName));
    }

    /**
     * Selects Carpet's standard {@link Rule} annotation.
     *
     * <pre>{@code
     * builder.onCarpetRuleAnnotation();
     * }</pre>
     *
     * @return this builder configured with the absolute Carpet annotation class name
     */
    default AnnotationAdapterBuilder onCarpetRuleAnnotation() {
        return ruleAnnotationClassName(
            PackageRef.absolute(Rule.class.getCanonicalName())
        );
    }
}

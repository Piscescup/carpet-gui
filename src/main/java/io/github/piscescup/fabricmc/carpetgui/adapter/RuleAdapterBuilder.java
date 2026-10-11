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


/**
 * Builds a {@link RuleClassCarpetAddonAdapter} for addons that keep rule objects in a collection.
 * The collection and each object's name are read through configurable {@link Accessor}s.
 *
 * <pre>{@code
 * CarpetAddonAdapter adapter = CarpetAddonAdapter.ruleBuilder("my_mod", "dev.example")
 *     .settingsClassName("MySettings")
 *     .carpetRuleClassName("RuleDefinition")
 *     .rulesAccessor(Accessor.ofStaticField("RULES"))
 *     .ruleNameAccessor(Accessor.<String>ofInstanceMethod("name").checked(String.class))
 *     .build();
 * }</pre>
 *
 * @author REN YuanTong
 * @since 1.0.0
 * @see RuleClassCarpetAddonAdapter
 * @see CarpetAddonAdapter#ruleBuilder(String, String)
 */
public interface RuleAdapterBuilder extends AdapterBuilder<RuleAdapterBuilder> {
    /**
     * Configures the class required for every object in the rules collection.
     *
     * @param packageRef absolute or parent-relative rule class reference
     * @return this builder
     * @throws NullPointerException if {@code packageRef} is {@code null}
     * @throws IllegalArgumentException if a relative reference has no parent package
     */
    RuleAdapterBuilder carpetRuleClassName(PackageRef packageRef);

    /**
     * Configures a parent-relative rule class name.
     *
     * <pre>{@code
     * builder.carpetRuleClassName("rules.RuleDefinition");
     * }</pre>
     *
     * @param carpetRuleClassName class name relative to the builder's parent package
     * @return this builder
     * @throws IllegalArgumentException if the name is blank or no parent package is available
     */
    default RuleAdapterBuilder carpetRuleClassName(String carpetRuleClassName) {
        return carpetRuleClassName(PackageRef.relative(carpetRuleClassName));
    }

    /**
     * Configures the accessor that returns the collection of rule objects.
     *
     * <pre>{@code
     * builder.rulesAccessor(Accessor.ofStaticField("RULES"));
     * }</pre>
     *
     * @param accessor accessor whose value must be a {@link java.util.Collection}
     * @return this builder
     * @throws NullPointerException if {@code accessor} is {@code null}
     */
    RuleAdapterBuilder rulesAccessor(
        Accessor<?> accessor
    );

    /**
     * Configures the accessor that extracts a name from each rule object.
     *
     * <pre>{@code
     * builder.ruleNameAccessor(Accessor.ofInstanceMethod("name"));
     * }</pre>
     *
     * @param accessor accessor whose value must be a rule name string
     * @return this builder
     * @throws NullPointerException if {@code accessor} is {@code null}
     */
    RuleAdapterBuilder ruleNameAccessor(
        Accessor<String> accessor
    );
}

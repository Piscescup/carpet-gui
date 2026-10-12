/*
 * This file is part of the Carpet GUI project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  PiscesCup and contributors
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


import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Discovers rule names from addon-specific objects held in a settings collection.
 * One accessor obtains the collection and another extracts a string name from every validated
 * rule object; the base adapter then resolves those names through the live Carpet manager.
 *
 * <pre>{@code
 * RuleClassCarpetAddonAdapter adapter = CarpetAddonAdapter
 *     .ruleBuilder("my_mod", "dev.example")
 *     .settingsClassName("MySettings")
 *     .carpetRuleClassName("RuleDefinition")
 *     .rulesAccessor(Accessor.ofStaticField("RULES"))
 *     .ruleNameAccessor(Accessor.ofInstanceMethod("name"))
 *     .build();
 * }</pre>
 *
 * @see RuleAdapterBuilder
 * @see Accessor
 * @see CarpetAddonAdapter
 */
public class RuleClassCarpetAddonAdapter extends CarpetAddonAdapter {
    private final PackageRef ruleClassRef;
    private final String carpetRuleCanonicalClassName;
    private final Accessor<?> rulesAccessor;
    private final Accessor<?> ruleNameAccessor;

    RuleClassCarpetAddonAdapter(
        RuleBuilder builder
    ) {
        super(builder);
        this.ruleClassRef = builder.ruleClassRef;
        this.carpetRuleCanonicalClassName = builder.ruleClassCanonicalName;
        this.rulesAccessor = builder.rulesAccessor;
        this.ruleNameAccessor = builder.ruleNameAccessor;
    }

    /**
     * Returns the unresolved rule-object class reference.
     *
     * @return the configured rule class reference
     */
    public PackageRef ruleClassRef() { return ruleClassRef; }

    /**
     * Returns the accessor used to obtain the rule collection.
     *
     * @return the configured collection accessor
     */
    public Accessor<?> rulesAccessor() { return rulesAccessor; }

    /**
     * Returns the accessor used to extract each rule's name.
     *
     * @return the configured rule-name accessor
     */
    public Accessor<?> ruleNameAccessor() { return ruleNameAccessor; }

    /**
     * Reads, validates, and de-duplicates names from the configured rule collection.
     *
     * @param args must be empty because method arguments belong in the configured accessor
     * @return an immutable ordered collection of non-blank rule names
     * @throws ClassNotFoundException if the configured settings or rule class cannot be loaded
     * @throws IllegalArgumentException if arguments are supplied or reflected values have the wrong type
     * @throws IllegalStateException if an accessor cannot read its configured member
     */
    @Override
    protected Collection<String> getRuleNames(Object... args) throws ClassNotFoundException {
        if (args != null && args.length != 0) {
            throw new IllegalArgumentException("Rule-name arguments must be configured in the Accessor");
        }
        Object target = rulesAccessor.isStaticAccess() ? getSettingsClass() : getSettingsInstance();
        if (target == null) return List.of();

        Object value = rulesAccessor.get(target);

        if (value == null) return List.of();

        if (!(value instanceof Collection<?> rules)) {
            throw new IllegalArgumentException("Rules accessor for " + modId
                + " must return a Collection, got " + value.getClass().getName());
        }

        Class<?> ruleClass = load(carpetRuleCanonicalClassName);
        var names = new LinkedHashSet<String>();

        for (Object rule : rules) {
            if (rule == null) continue;
            if (!ruleClass.isInstance(rule)) {
                throw new IllegalArgumentException("Rules for " + modId + " must be instances of "
                    + ruleClass.getName() + ", got " + rule.getClass().getName());
            }
            Object name = ruleNameAccessor.get(ruleNameAccessor.isStaticAccess() ? ruleClass : rule);
            if (name == null) continue;
            if (!(name instanceof String text)) {
                throw new IllegalArgumentException("Rule-name accessor for " + modId
                    + " must return a String, got " + name.getClass().getName());
            }
            if (!text.isBlank()) names.add(text);
        }

        return List.copyOf(names);
    }

}

/** Package-private implementation of {@link RuleAdapterBuilder}. */
class RuleBuilder
    extends Builder<RuleAdapterBuilder>
    implements RuleAdapterBuilder
{
    PackageRef ruleClassRef;
    String ruleClassCanonicalName;

    Accessor<?> rulesAccessor;

    Accessor<?> ruleNameAccessor;

    protected RuleBuilder(String modId, String packageName) {
        super(modId, packageName);
    }

    @Override
    public RuleAdapterBuilder carpetRuleClassName(PackageRef packageRef) {
        this.ruleClassRef = Objects.requireNonNull(packageRef, "Class reference");
        this.ruleClassCanonicalName =
            packageRef.toCanonicalPackage(this.packageName);
        return this;
    }

    @Override
    public RuleAdapterBuilder rulesAccessor(Accessor<?> accessor) {
        this.rulesAccessor = Objects.requireNonNull(accessor, "Rules accessor");
        return this;
    }

    @Override
    public RuleAdapterBuilder ruleNameAccessor(Accessor<String> accessor) {
        this.ruleNameAccessor = Objects.requireNonNull(accessor, "Rule-name accessor");
        return this;
    }

    @Override
    public RuleClassCarpetAddonAdapter build() {
        requireText(modId, "Mod ID");
        requireText(carpetSettingsClassCanonicalName, "Settings class");
        requireText(ruleClassCanonicalName, "Rule class");
        Objects.requireNonNull(rulesAccessor, "Rules accessor");
        Objects.requireNonNull(ruleNameAccessor, "Rule-name accessor");
        if (!rulesAccessor.isStaticAccess()) {
            Objects.requireNonNull(settingsAccessor, "Settings accessor");
        }
        if (carpetExtensionClassCanonicalName != null) {
            Objects.requireNonNull(extensionAccessor, "Extension accessor");
        }
        return new RuleClassCarpetAddonAdapter(this);
    }
    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
    }

}

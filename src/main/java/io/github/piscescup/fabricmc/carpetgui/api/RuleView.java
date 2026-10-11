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

package io.github.piscescup.fabricmc.carpetgui.api;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

/**
 * Read-only, backend-independent presentation model for one rule.
 * Values are represented as text so the GUI can display Carpet rules, vanilla game rules, and
 * custom sources through the same contract.
 *
 * <pre>{@code
 * RuleView rule = ...;
 * drawRule(rule.label(), rule.description(), rule.value());
 * if (rule.modified()) drawResetIndicator();
 * }</pre>
 *
 * @see EditableRuleView
 * @see RuleSource
 */
public interface RuleView {
    /**
     * Returns the rule's local identifier.
     *
     * <pre>{@code
     * String ruleName = rule.id();
     * }</pre>
     *
     * @return the identifier used by the owning rule system
     */
    String id();

    /**
     * Returns the stable presentation identity used for client-side state. It may include an owner
     * or manager when local rule IDs are not globally unique.
     *
     * <pre>{@code
     * expandedRows.put(rule.stateId(), true);
     * }</pre>
     *
     * @return the stable state key; {@link #id()} by default
     */
    default String stateId() { return id(); }

    /**
     * Returns the rule's display label.
     *
     * <pre>{@code
     * drawLabel(rule.label());
     * }</pre>
     *
     * @return the localized or literal label
     */
    Component label();

    /**
     * Returns the primary explanatory text for the rule.
     *
     * <pre>{@code
     * showTooltip(rule.description());
     * }</pre>
     *
     * @return the localized or literal description
     */
    Component description();

    /**
     * Returns the rule's current value in its editable textual form.
     *
     * <pre>{@code
     * textBox.setValue(rule.value());
     * }</pre>
     *
     * @return the current textual value
     */
    String value();

    /**
     * Returns the value declared by the rule implementation before server configuration is applied.
     *
     * <pre>{@code
     * resetButton.active = !rule.value().equals(rule.defaultValue());
     * }</pre>
     *
     * @return the declared initial value in textual form
     */
    String defaultValue();

    /**
     * Returns the effective startup value supplied by the current server configuration, when known.
     *
     * <pre>{@code
     * String baseline = rule.configuredValue().orElse(rule.defaultValue());
     * }</pre>
     *
     * @return the configured textual value, or an empty optional when it is unavailable
     */
    default Optional<String> configuredValue() {
        return Optional.empty();
    }

    /**
     * Indicates whether this rule is explicitly present in the current world's Carpet
     * configuration.
     *
     * <pre>{@code
     * if (rule.explicitlyConfigured()) drawSavedMarker();
     * }</pre>
     *
     * @return {@code true} when the configuration explicitly contains this rule
     */
    default boolean explicitlyConfigured() { return false; }

    /**
     * Returns the categories used to group and filter the rule.
     *
     * <pre>{@code
     * boolean isCreative = rule.categories().contains("creative");
     * }</pre>
     *
     * @return the ordered category identifiers; may be empty
     */
    List<String> categories();

    /**
     * Returns additional description lines displayed after the primary description.
     *
     * <pre>{@code
     * rule.extraInfo().forEach(this::drawInfoLine);
     * }</pre>
     *
     * @return the extra information components; empty by default
     */
    default List<Component> extraInfo() { return List.of(); }

    /**
     * Returns searchable text independent of the active GUI language. Backends may override this
     * method to include aliases or translations from additional languages.
     *
     * <pre>{@code
     * boolean matches = rule.searchTerms().stream().anyMatch(term -> term.contains(query));
     * }</pre>
     *
     * @return the strings considered by rule search
     */
    default List<String> searchTerms() {
        return List.of(id(), label().getString(), description().getString(), String.join(" ", categories()));
    }

    /**
     * Indicates whether the current value differs from {@link #defaultValue()}.
     *
     * <pre>{@code
     * if (rule.modified()) drawModifiedDot();
     * }</pre>
     *
     * @return {@code true} when the current and declared initial values differ
     */
    default boolean modified() {
        return !value().equals(defaultValue());
    }

    /**
     * Indicates whether the current value differs from the value declared by the rule
     * implementation.
     *
     * <pre>{@code
     * boolean changedFromCodeDefault = rule.differsFromInitialValue();
     * }</pre>
     *
     * @return {@link #modified()} by default
     */
    default boolean differsFromInitialValue() {
        return modified();
    }

    /**
     * Indicates whether the current value differs from the effective configuration loaded by the
     * server.
     *
     * <pre>{@code
     * if (rule.differsFromConfiguredValue()) drawRuntimeOverrideMarker();
     * }</pre>
     *
     * @return {@code true} when a configured value is known and differs from {@link #value()}
     */
    default boolean differsFromConfiguredValue() {
        return configuredValue().map(configured -> !value().equals(configured)).orElse(false);
    }
}

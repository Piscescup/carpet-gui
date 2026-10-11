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

package io.github.piscescup.fabricmc.carpetgui.api;

import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleEditResult;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Extends {@link RuleEditor} with the ability to persist a value as the server configuration
 * default. A saved default is distinct from both the rule's declared initial value and an ordinary
 * runtime edit.
 *
 * <pre>{@code
 * if (editor instanceof PersistentRuleEditor persistent && persistent.canSaveDefault()) {
 *     persistent.saveDefault(rule.value());
 * }
 * }</pre>
 *
 * @see RuleView#configuredValue()
 * @see RuleView#explicitlyConfigured()
 */
public interface PersistentRuleEditor extends RuleEditor {
    /**
     * Indicates whether the current client may persist a default value.
     *
     * <pre>{@code
     * saveButton.active = editor.canSaveDefault();
     * }</pre>
     *
     * @return {@code true} when saving a default is currently allowed
     */
    boolean canSaveDefault();

    /**
     * Whether this value has been acknowledged as a saved default by the backend.
     *
     * <pre>{@code
     * boolean saved = editor.isSavedDefault(rule.value());
     * }</pre>
     *
     * @param value the textual rule value to compare with the acknowledged default
     * @return {@code true} when the backend confirms that {@code value} is the saved default;
     *         {@code false} by default
     */
    default boolean isSavedDefault(String value) {
        return false;
    }

    /**
     * Returns the explanation shown when {@link #canSaveDefault()} is {@code false}.
     *
     * <pre>{@code
     * if (!editor.canSaveDefault()) showTooltip(editor.defaultDisabledReason());
     * }</pre>
     *
     * @return the disabled-state explanation, or an empty component when none is needed
     */
    Component defaultDisabledReason();

    /**
     * Persists a textual value as this rule's server configuration default.
     *
     * <pre>{@code
     * RuleEditResult result = editor.saveDefault("true");
     * }</pre>
     *
     * @param value the textual value to persist
     * @return the immediate acceptance, queueing, or rejection result
     * @throws RuntimeException if the backend cannot validate, dispatch, or persist the value
     */
    RuleEditResult saveDefault(String value);

    /**
     * Persists a value and optionally reports a later asynchronous acknowledgement. Legacy
     * backends use the default implementation and do not invoke the callback.
     *
     * <pre>{@code
     * RuleEditResult immediate = editor.saveDefault("true",
     *     result -> showFeedback(result.message()));
     * }</pre>
     *
     * @param value the textual value to persist
     * @param completed callback for a later backend result when asynchronous acknowledgement is supported
     * @return the immediate acceptance, queueing, or rejection result
     * @throws RuntimeException if the backend cannot validate, dispatch, or persist the value
     */
    default RuleEditResult saveDefault(String value, Consumer<RuleEditResult> completed) {
        return saveDefault(value);
    }
}

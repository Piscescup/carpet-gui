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

import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleEditResult;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * Defines the backend-controlled editing contract for a rule.
 * The GUI obtains input hints and submits textual values through this interface; it never writes
 * the underlying rule field directly.
 *
 * <pre>{@code
 * RuleEditor editor = editableRule.editor();
 * if (editor.editable()) {
 *     RuleEditResult result = editor.submit("false");
 * }
 * }</pre>
 *
 * @see EditableRuleView
 * @see PersistentRuleEditor
 * @see RuleEditResult
 */
public interface RuleEditor {
    /** Describes the input control that best matches the rule's value type. */
    enum InputKind {
        /** A two-state boolean control. */
        BOOLEAN,
        /** A numeric text field. */
        NUMBER,
        /** An unrestricted text field. */
        TEXT
    }

    /**
     * Returns the preferred kind of input control.
     *
     * <pre>{@code
     * boolean checkbox = editor.inputKind() == RuleEditor.InputKind.BOOLEAN;
     * }</pre>
     *
     * @return the input kind for this rule
     */
    InputKind inputKind();

    /**
     * Returns suggested textual values for completion or selection.
     *
     * <pre>{@code
     * editor.suggestions().forEach(textBox::suggest);
     * }</pre>
     *
     * @return the ordered suggestions, or an empty list when none are available
     */
    List<String> suggestions();

    /**
     * Indicates whether input is restricted to {@link #suggestions()}.
     *
     * <pre>{@code
     * if (editor.strict() && !editor.suggestions().contains(value)) return;
     * }</pre>
     *
     * @return {@code true} when arbitrary values must be rejected by the UI
     */
    boolean strict();

    /**
     * Indicates whether the current client may submit edits.
     *
     * <pre>{@code
     * submitButton.active = editor.editable();
     * }</pre>
     *
     * @return {@code true} when editing is currently allowed
     */
    boolean editable();

    /**
     * Returns the explanation shown when {@link #editable()} is {@code false}.
     *
     * <pre>{@code
     * if (!editor.editable()) showTooltip(editor.disabledReason());
     * }</pre>
     *
     * @return the disabled-state explanation, or an empty component when no explanation is needed
     */
    Component disabledReason();

    /**
     * Validates and submits a new textual rule value.
     *
     * <pre>{@code
     * RuleEditResult result = editor.submit("42");
     * if (!result.accepted()) showFeedback(result.message());
     * }</pre>
     *
     * @param value the value exactly as entered or selected by the user
     * @return the immediate acceptance, queueing, or rejection result
     * @throws RuntimeException if the backend cannot validate or dispatch the edit
     */
    RuleEditResult submit(String value);

    /**
     * Submits a new value and optionally reports a later asynchronous acknowledgement. Legacy
     * backends use the default implementation, return their immediate result, and do not invoke
     * the callback.
     *
     * <pre>{@code
     * RuleEditResult immediate = editor.submit("42", result -> showFeedback(result.message()));
     * }</pre>
     *
     * @param value the value exactly as entered or selected by the user
     * @param completed callback for a later backend result when asynchronous acknowledgement is supported
     * @return the immediate acceptance, queueing, or rejection result
     * @throws RuntimeException if the backend cannot validate or dispatch the edit
     */
    default RuleEditResult submit(String value, Consumer<RuleEditResult> completed) {
        return submit(value);
    }
}

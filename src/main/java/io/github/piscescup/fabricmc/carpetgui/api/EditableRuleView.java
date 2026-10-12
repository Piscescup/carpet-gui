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

/**
 * Extends a read-only {@link RuleView} with a backend-provided editing capability.
 * Keeping the editor separate lets callers display every rule while enabling controls only for
 * rules whose backend can safely apply changes.
 *
 * <pre>{@code
 * if (rule instanceof EditableRuleView editable && editable.editor().editable()) {
 *     editable.editor().submit("true");
 * }
 * }</pre>
 *
 * @see RuleEditor
 * @see PersistentRuleEditor
 */
public interface EditableRuleView extends RuleView {
    /**
     * Returns the editor responsible for validating and submitting changes to this rule.
     *
     * <pre>{@code
     * RuleEditResult result = editableRule.editor().submit(newValue);
     * }</pre>
     *
     * @return the rule's editing backend
     */
    RuleEditor editor();
}

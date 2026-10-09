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

//#if MC >= 260000
package io.github.piscescup.fabricmc.carpetgui.gui.widget;

import io.github.piscescup.fabricmc.carpetgui.gui.layout.GuiBounds;
import io.github.piscescup.fabricmc.carpetgui.gui.model.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Reusable inline presentation for any rule backend exposing RuleEditor; no Carpet dependency.
 */
public final class InlineRuleControl {
    private final RuleView rule;
    private final Consumer<RuleEditResult> feedback;
    private final AbstractWidget widget;
    private final DropdownWidget<Choice> dropdown;
    private final RuleTextBox textBox;

    public InlineRuleControl(Font font, GuiBounds bounds, RuleView rule, Supplier<GuiBounds> popupArea, Consumer<RuleEditResult> feedback) {
        this.rule = rule;
        this.feedback = feedback;
        DropdownWidget<Choice> choices = null;
        RuleTextBox text = null;
        if (rule instanceof EditableRuleView editable) {
            RuleEditor editor = editable.editor();
            if (editor.inputKind() == RuleEditor.InputKind.BOOLEAN) {
                widget = new GuiButton(
                    bounds.left(),
                    bounds.top(),
                    bounds.width(),
                    bounds.height(),
                    valueLabel(),
                    ignored -> submit(Boolean.toString(!Boolean.parseBoolean(rule.value())))
                );
            } else if (editor.strict() && !editor.suggestions()
                .isEmpty()) {
                var values = new LinkedHashSet<>(editor.suggestions());
                values.add(rule.value());
                choices = new DropdownWidget<>(
                    bounds, rule.label(),
                    values.stream()
                        .map(value -> new Choice(value, choiceLabel(editor, value)))
                        .toList(), rule.value(),
                    popupArea, option -> submit(option.id())
                );
                widget = choices;
            } else {
                text = new RuleTextBox(font, bounds, editable, feedback);
                widget = text;
            }
        } else {
            widget = new GuiButton(
                bounds.left(), bounds.top(), bounds.width(), bounds.height(), valueLabel(), ignored -> {
            }
            );
        }
        dropdown = choices;
        textBox = text;
    }

    public AbstractWidget widget() {
        return widget;
    }

    public DropdownWidget<?> dropdown() {
        return dropdown;
    }

    public boolean editing() {
        return textBox != null && (textBox.isFocused() || textBox.hasDraft()) || dropdown != null && dropdown.isOpen();
    }

    public void cancelDraft() {
        if (textBox != null) textBox.cancelDraft();
    }

    public void submit(String value) {
        if (rule instanceof EditableRuleView editable) {
            feedback.accept(editable.editor()
                .submit(value, feedback));
            refresh(widget.active);
        }
    }

    public RuleEditResult saveDefault() {
        if (!(rule instanceof EditableRuleView editable) || !(editable.editor() instanceof PersistentRuleEditor editor)) {
            return RuleEditResult.rejected(Component.translatable("carpet-gui.edit.default_unsupported"));
        }
        String value = textBox != null && textBox.hasDraft() ? textBox.getValue() : rule.value();
        RuleEditResult result = editor.saveDefault(value, feedback);
        feedback.accept(result);
        if (result.accepted()) cancelDraft();
        refresh(widget.active);
        return result;
    }

    public void refresh(boolean interactive) {
        widget.active = interactive && rule instanceof EditableRuleView editable && editable.editor()
            .editable();
        if (textBox != null) {
            textBox.setEditable(widget.active);
            if (!widget.active) textBox.cancelDraft();
            textBox.synchronizeValue();
        } else if (dropdown != null) {
            if (!widget.active) dropdown.closePopup();
            if (!dropdown.isOpen()) dropdown.synchronizeSelection(rule.value());
        } else {
            widget.setMessage(valueLabel());
        }
    }

    private Component choiceLabel(RuleEditor editor, String value) {
        if (editor.inputKind() == RuleEditor.InputKind.TEXT && rule.categories().contains("command")) {
            return Choice.PERMISSIONS.stream()
                .filter(option -> option.id().equals(value))
                .map(Choice::label)
                .findFirst()
                .orElseGet(() -> Component.literal(value));
        }
        return Component.literal(value);
    }

    private Component valueLabel() {
        var label = Component.literal(rule.value());
        if (rule.value()
            .equals("true")) {
            label.withStyle(ChatFormatting.GREEN);
        }
        if (rule.value()
            .equals("false")) {
            label.withStyle(ChatFormatting.RED);
        }
        return label;
    }

    private record Choice(
        String id,
        Component label
    )
        implements DropdownOption
    {
        public static final List<Choice> EMPTY = List.of();

        public static final List<Choice> PERMISSIONS = List.of(
            new Choice("false", Component.literal("false")),
            new Choice("true", Component.literal("true")),
            new Choice("ops", Component.literal("op")),
            new Choice("0", Component.literal("all")),
            new Choice("1", Component.literal("moderator")),
            new Choice("2", Component.literal("game master")),
            new Choice("3", Component.literal("admin")),
            new Choice("4", Component.literal("owner"))
        );
    }
}
//#endif

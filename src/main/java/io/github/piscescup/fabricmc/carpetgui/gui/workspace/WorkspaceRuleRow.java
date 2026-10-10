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

//#if MC >= 12111
package io.github.piscescup.fabricmc.carpetgui.gui.workspace;

import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.label.TLabelElement;
import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
import com.thecsdev.commonmc.api.client.gui.tooltip.TTooltip;
import io.github.piscescup.fabricmc.carpetgui.gui.model.EditableRuleView;
import io.github.piscescup.fabricmc.carpetgui.gui.model.PersistentRuleEditor;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleEditResult;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleEditor;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleView;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.button.Button;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.NativeTextInput;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.button.FavoriteButton;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.icons.RuleActionIcon;
import io.github.piscescup.fabricmc.carpetgui.store.FavoriteRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * One live rule: presentation and drafts only, all mutations go through the existing RuleEditor.
 */
final class WorkspaceRuleRow
    extends TElement
{
    private static final Map<String, String> PERMISSIONS = Map.of(
        "ops", "op", "0", "all", "1", "moderator", "2", "game master", "3", "admin", "4", "owner");
    private final String modId;
    private final RuleView rule;
    private final Consumer<RuleEditResult> feedback;
    private final Consumer<String> discardDrafts;
    private final IntSupplier statusColor;
    private final Supplier<Component> detailText;
    private final boolean coloredStatus;
    private TLabelElement title;
    private TLabelElement description;
    private TElement valueControl;
    private NativeTextInput text;
    private WorkspaceStyle.Dropdown<String> choices;
    private Button valueButton;
    private RuleActionIcon reset;
    private RuleActionIcon saveDefault;
    private FavoriteButton favorite;
    private String observed;
    private String lastAttempt;
    private boolean dirty;
    private boolean synchronizing;
    private boolean suppressBlur;
    private String pendingSaveValue;
    private boolean deferOffscreen;

    void deferOffscreenControls() {
        deferOffscreen = true;
    }

    private boolean nearViewport() {
        var parent = parentProperty().get();
        if (parent == null) return true;
        var viewport = parent.getBounds();
        var bounds = getBounds();
        return bounds.endY >= viewport.y - viewport.height
            && bounds.y <= viewport.endY + viewport.height;
    }

    WorkspaceRuleRow(String modId, RuleView rule, Consumer<RuleEditResult> feedback, Consumer<String> discardDrafts) {
        this(modId, rule, feedback, discardDrafts, () -> WorkspaceStyle.TEXT_COLOR, rule::description, false);
    }

    WorkspaceRuleRow(String modId, RuleView rule, Consumer<RuleEditResult> feedback, Consumer<String> discardDrafts,
                     IntSupplier statusColor) {
        this(modId, rule, feedback, discardDrafts, statusColor, rule::description, true);
    }

    WorkspaceRuleRow(String modId, RuleView rule, Consumer<RuleEditResult> feedback, Consumer<String> discardDrafts,
                     IntSupplier statusColor, Supplier<Component> detailText) {
        this(modId, rule, feedback, discardDrafts, statusColor, detailText, true);
    }

    private WorkspaceRuleRow(String modId, RuleView rule, Consumer<RuleEditResult> feedback, Consumer<String> discardDrafts,
                             IntSupplier statusColor, Supplier<Component> detailText, boolean coloredStatus) {
        this.modId = modId;
        this.rule = rule;
        this.feedback = feedback;
        this.discardDrafts = discardDrafts;
        this.statusColor = statusColor;
        this.detailText = detailText;
        this.coloredStatus = coloredStatus;
        observed = rule.value();
        tooltipProperty().set(ignored -> tooltip(), WorkspaceRuleRow.class);
    }

    @Override
    protected void initCallback() {
        if (deferOffscreen && !nearViewport()) return;
        var bounds = getBounds();
        RuleEditor editor = rule instanceof EditableRuleView editable ? editable.editor() : null;
        boolean persistent = editor instanceof PersistentRuleEditor;
        int resetWidth = 20;
        int saveWidth = persistent ? 20 : 0;
        int favoriteWidth = 20;
        int valueWidth = Math.clamp(bounds.width / 8, 48, 76);
        int favoriteX = bounds.endX - favoriteWidth - 4;
        int resetX = favoriteX - resetWidth - 4;
        int saveX = persistent ? resetX - saveWidth - 4 : resetX;
        int valueX = saveX - valueWidth - 4;
        title = WorkspaceStyle.label(this, rule.label(), bounds.x + 5, bounds.y + 3, valueX - bounds.x - 10, 10, statusColor.getAsInt());
        title.wrapTextProperty()
            .set(false, WorkspaceRuleRow.class);
        description = WorkspaceStyle.label(
            this, detailText.get(), bounds.x + 5, bounds.y + 14,
            valueX - bounds.x - 10, 9, WorkspaceStyle.MUTED_COLOR
        );
        description.wrapTextProperty()
            .set(false, WorkspaceRuleRow.class);
        description.textScaleProperty()
            .set(WorkspaceStyle.SMALL_TEXT_SCALE, WorkspaceRuleRow.class);

        if (editor != null && editor.inputKind() == RuleEditor.InputKind.BOOLEAN) {
            valueButton = new Button(
                valueLabel(rule.value()),
                () -> submit(Boolean.toString(!Boolean.parseBoolean(rule.value())))
            );
            valueControl = valueButton;
        } else if (editor != null && editor.strict() && !editor.suggestions()
            .isEmpty()) {
            choices = new WorkspaceStyle.Dropdown<>(new WorkspaceStyle.Option<>(rule.value(), valueLabel(rule.value())));
            LinkedHashSet<String> values = new LinkedHashSet<>(editor.suggestions());
            values.add(rule.value());
            values.forEach(value -> choices.getEntries()
                .add(new WorkspaceStyle.Option<>(value, valueLabel(value))));
            choices.selectedEntryProperty()
                .addChangeListener((property, previous, selected) -> {
                    if (!synchronizing && selected != null) submit(selected.value());
                });
            valueControl = choices;
        } else if (editor != null) {
            text = new NativeTextInput(
                rule.label(), rule.value(), value -> {
                if (synchronizing) return;
                dirty = !value.equals(observed);
                lastAttempt = null;
            }, () -> {
                if (!suppressBlur) commit();
                suppressBlur = false;
            }, this::cancelAndUnfocus
            );
            valueControl = text;
        } else {
            valueButton = new Button(
                valueLabel(rule.value()), () -> {
            }
            );
            valueControl = valueButton;
        }
        valueControl.setBounds(valueX, bounds.y + 2, valueWidth, WorkspaceStyle.CONTROL_HEIGHT);
        valueControl.tooltipProperty()
            .set(ignored -> tooltip(), WorkspaceRuleRow.class);
        add(valueControl);
        reset = new RuleActionIcon(false, () -> false, () -> {
            discardDrafts.accept(rule.stateId());
            submit(rule.defaultValue());
        }
        );
        reset.setBounds(resetX, bounds.y + 2, resetWidth, WorkspaceStyle.CONTROL_HEIGHT);
        reset.tooltipProperty()
            .set(ignored -> tooltip(), WorkspaceRuleRow.class);
        add(reset);
        if (persistent) {
            saveDefault = new RuleActionIcon(true,
                () -> ((PersistentRuleEditor) ((EditableRuleView) rule).editor()).isSavedDefault(draftValue()), this::saveDefault);
            saveDefault.setBounds(saveX, bounds.y + 2, saveWidth, WorkspaceStyle.CONTROL_HEIGHT);
            saveDefault.tooltipProperty()
                .set(ignored -> TTooltip.of(Component.translatable("carpet-gui.set_default_hint")), WorkspaceRuleRow.class);
            add(saveDefault);
        }
        favorite = new FavoriteButton(
            () -> FavoriteRules.contains(rule.stateId()),
            () -> {
                FavoriteRules.toggle(rule.stateId());
                favorite.invalidateTooltipCache();
                var screen = screenProperty().get();
                if (screen instanceof CarpetWorkspaceScreen workspace) workspace.requestListRefresh();
            }
        );
        favorite.setBounds(favoriteX, bounds.y + 2, favoriteWidth, WorkspaceStyle.CONTROL_HEIGHT);
        favorite.tooltipProperty()
            .set(
                ignored -> TTooltip.of(Component.translatable(
                    FavoriteRules.contains(rule.stateId()) ? "carpet-gui.favorite.remove" : "carpet-gui.favorite.add"
                )), WorkspaceRuleRow.class
            );
        add(favorite);
        refresh();
    }

    boolean interacting() {
        return dirty || text != null && text.isFocused();
    }

    String stateId() {
        return rule.stateId();
    }

    boolean resetHit(double x, double y) {
        return reset != null && reset.enabledProperty()
            .getZ() && inside(reset, x, y);
    }

    boolean saveHit(double x, double y) {
        return saveDefault != null && saveDefault.enabledProperty()
            .getZ() && inside(saveDefault, x, y);
    }

    boolean focusedText() {
        return text != null && text.isFocused();
    }

    String draftValue() {
        return text != null && dirty ? text.value() : rule.value();
    }

    void suppressNextBlur() {
        suppressBlur = true;
    }

    void prepareSave(String value) {
        pendingSaveValue = value;
    }

    void clearPendingSave() {
        pendingSaveValue = null;
    }

    private static boolean inside(TElement element, double x, double y) {
        if (element == null) return false;
        var bounds = element.getBounds();
        return x >= bounds.x && x < bounds.endX && y >= bounds.y && y < bounds.endY;
    }

    void cancelDraft() {
        dirty = false;
        lastAttempt = null;
        if (text != null) synchronizeText();
    }

    private void cancelAndUnfocus() {
        cancelDraft();
        var screen = screenProperty().get();
        if (screen != null) {
            screen.focusedElementProperty()
                .set(null, WorkspaceRuleRow.class);
        }
    }

    private void commit() {
        if (text == null || !dirty || !editable() || text.value()
            .equals(lastAttempt)) {
            return;
        }
        if (text.value()
            .equals(rule.value())) {
            cancelDraft();
            return;
        }
        lastAttempt = text.value();
        if (submit(lastAttempt).accepted()) cancelDraft();
    }

    private RuleEditResult submit(String value) {
        if (!(rule instanceof EditableRuleView editable)) return RuleEditResult.rejected(Component.empty());
        RuleEditResult result = editable.editor()
            .submit(value, feedback);
        feedback.accept(result);
        refresh();
        return result;
    }

    private void saveDefault() {
        suppressBlur = false;
        if (!(rule instanceof EditableRuleView editable) || !(editable.editor() instanceof PersistentRuleEditor editor)) return;
        String value = pendingSaveValue != null ? pendingSaveValue : draftValue();
        pendingSaveValue = null;
        RuleEditResult result = editor.saveDefault(value, feedback);
        feedback.accept(result);
        if (result.accepted()) discardDrafts.accept(rule.stateId());
    }

    private boolean editable() {
        return rule instanceof EditableRuleView editable && editable.editor()
            .editable();
    }

    private void synchronizeText() {
        observed = rule.value();
        synchronizing = true;
        try {
            text.setValue(observed);
        } finally {
            synchronizing = false;
        }
    }

    void refresh() {
        if (deferOffscreen) {
            if (!nearViewport() && !interacting()) return;
            if (valueControl == null) {
                clearAndInit();
                return;
            }
        }
        if (valueControl == null) return;
        if (title != null) title.textColorProperty().set(statusColor.getAsInt(), WorkspaceRuleRow.class);
        if (description != null) description.setText(detailText.get());
        boolean editable = editable();
        if (text != null) {
            text.setEnabled(editable);
            if (!editable) {
                cancelDraft();
            } else if (!dirty) synchronizeText();
        } else if (valueButton != null) {
            valueButton.enabledProperty()
                .set(editable, WorkspaceRuleRow.class);
            valueButton.getLabel()
                .setText(valueLabel(rule.value()));
        } else if (choices != null) {
            choices.enabledProperty()
                .set(editable, WorkspaceRuleRow.class);
            var selected = choices.selectedEntryProperty()
                .get();
            if (selected == null || !selected.value()
                .equals(rule.value())) {
                synchronizing = true;
                try {
                    choices.selectedEntryProperty()
                        .set(new WorkspaceStyle.Option<>(rule.value(), valueLabel(rule.value())), WorkspaceRuleRow.class);
                } finally {
                    synchronizing = false;
                }
            }
        }
        reset.enabledProperty()
            .set(editable && (rule.modified() || dirty), WorkspaceRuleRow.class);
        if (saveDefault != null) {
            var editor = (PersistentRuleEditor) ((EditableRuleView) rule).editor();
            saveDefault.enabledProperty()
                .set(editor.canSaveDefault(), WorkspaceRuleRow.class);
        }
        invalidateTooltipCache();
        valueControl.invalidateTooltipCache();
    }

    private Component valueLabel(String value) {
        RuleEditor editor = rule instanceof EditableRuleView editable ? editable.editor() : null;
        String label = editor != null && editor.inputKind() == RuleEditor.InputKind.TEXT
                       && rule.categories()
                           .stream()
                           .anyMatch(category -> category.equalsIgnoreCase("command"))
            ? PERMISSIONS.getOrDefault(value, value) : value;
        return Component.literal(label)
            .withStyle(value.equals("true") ? ChatFormatting.GREEN
                : value.equals("false") ? ChatFormatting.RED : ChatFormatting.WHITE);
    }

    private TTooltip tooltip() {
        String key = rule.id()
            .contains(":") ? rule.id() : modId + ":" + rule.id();
        var text = rule.label()
            .copy()
            .withStyle(ChatFormatting.YELLOW)
            .append(Component.literal("\nKey: " + key + "\nValue: " + rule.value())
                .withStyle(ChatFormatting.GRAY));
        if (!rule.description()
            .getString()
            .isBlank()) {
            text.append(Component.literal("\n\n"))
                .append(rule.description()
                    .copy()
                    .withStyle(ChatFormatting.WHITE));
        }
        for (Component info : rule.extraInfo())
            text.append(Component.literal("\n"))
                .append(info.copy()
                    .withStyle(ChatFormatting.GRAY));
        if (rule instanceof EditableRuleView editable && !editable.editor()
            .editable()) {
            text.append(Component.literal("\n"))
                .append(editable.editor()
                    .disabledReason()
                    .copy()
                    .withStyle(ChatFormatting.RED));
        }
        return TTooltip.of(text);
    }

    @Override
    public void renderCallback(TGuiGraphics graphics) {
        var bounds = getBounds();
        graphics.fillColor(bounds.x, bounds.y, bounds.width, bounds.height, WorkspaceStyle.RULE_BACKGROUND_COLOR);
        graphics.drawOutlineIn(bounds.x, bounds.y, bounds.width, bounds.height, WorkspaceStyle.BORDER_COLOR);
        if (coloredStatus) graphics.fillColor(bounds.x + 1, bounds.y + 1, 3, Math.max(0, bounds.height - 2), statusColor.getAsInt());
    }

    @Override
    public void postRenderCallback(TGuiGraphics graphics) {
        if (isHoveredOrFocused() || findChild(TElement::isHoveredOrFocused, true).isPresent()) {
            var bounds = getBounds();
            graphics.drawOutlineIn(bounds.x, bounds.y, bounds.width, bounds.height, WorkspaceStyle.FOCUS_COLOR);
        }
    }
}
//#endif

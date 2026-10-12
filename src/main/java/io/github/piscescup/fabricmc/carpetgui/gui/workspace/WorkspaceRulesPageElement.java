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

//#if MC >= 12111
package io.github.piscescup.fabricmc.carpetgui.gui.workspace;

import com.thecsdev.common.util.enumerations.CompassDirection;
import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.label.TLabelElement;
import com.thecsdev.commonmc.api.client.gui.panel.TPanelElement;
import com.thecsdev.commonmc.api.client.gui.tooltip.TTooltip;
import com.thecsdev.commonmc.api.client.gui.widget.TCheckboxWidget;
import io.github.piscescup.fabricmc.carpetgui.gui.GUIStyle;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleBrowserModel;
import io.github.piscescup.fabricmc.carpetgui.gui.pages.RulePage;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.NativeTextInput;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.button.ControlButton;
import io.github.piscescup.fabricmc.carpetgui.network.ClientRuleConfigurations;
import io.github.piscescup.fabricmc.carpetgui.store.FavoriteRules;
import io.github.piscescup.fabricmc.carpetgui.store.WorkspaceIcon;
import io.github.piscescup.fabricmc.carpetgui.util.MsgUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

/** Rule-browser page layout and filter controls. */
final class WorkspaceRulesPageElement extends WorkspacePageElement {
    private final RulePage page;

    WorkspaceRulesPageElement(WorkspaceDocumentInterface workspace, RulePage page) {
        super(workspace);
        this.page = page;
    }

    @Override
    protected void initCallback() {
        var bounds = getBounds();
        RuleBrowserModel model = RuleBrowserModel.forPage(page.id());
        List<String> categories = model.categories(page);
        if (RuleBrowserModel.FAVORITES.equals(model.category)) {
            model.category = "";
            model.favoritesOnly = true;
        }
        if (!model.category.isEmpty() && !categories.contains(model.category)) {
            model.category = "";
        }
        int sidebarWidth = Math.clamp(bounds.width * 30 / 100, Math.min(120, bounds.width / 3), 235);
        var sidebar = new WorkspacePanel(12);
        workspace.addWorkspacePane(sidebar, bounds.x, bounds.y, sidebarWidth, bounds.height);
        buildSidebar(sidebar, model, categories);

        int listX = bounds.x + sidebarWidth;
        int listWidth = Math.max(1, bounds.width - sidebarWidth);
        TLabelElement counts = WorkspaceStyle.label(this, Component.empty(), listX + 10, bounds.y + 8,
            listWidth - 20, 18, GUIStyle.MUTED_COLOR);
        RuleListPanel ruleList = new RuleListPanel(workspace.screen(), page, model);
        workspace.addWorkspacePane(ruleList, listX, bounds.y + 23, listWidth, Math.max(1, bounds.height - 23));
        workspace.registerRuleListState(ruleList, counts, categories);
    }

    private void buildSidebar(TPanelElement sidebar, RuleBrowserModel model, List<String> categories) {
        var bounds = sidebar.getBounds();
        int x = bounds.x + 9, width = Math.max(1, bounds.width - 18), y = bounds.y + 9;
        var filterTitle = WorkspaceStyle.label(sidebar, MsgUtils.tr("filters"), x, y, width, 18, GUIStyle.TEXT_COLOR);
        filterTitle.textAlignmentProperty().set(CompassDirection.CENTER, WorkspaceRulesPageElement.class);
        y += 23;

        List<WorkspaceStyle.Option<String>> categoryOptions = new ArrayList<>();
        categoryOptions.add(new WorkspaceStyle.Option<>("", MsgUtils.tr("all_categories")));
        categories.forEach(category -> categoryOptions.add(
            new WorkspaceStyle.Option<>(category, workspace.categoryLabel(page, category))));
        addDropdown(sidebar, x, y, width, categoryOptions, model.category, category -> {
            model.category = category;
            workspace.requestListRefresh(true);
        });
        y += 25;

        var search = new NativeTextInput(MsgUtils.tr("search"), model.query, query -> {
            model.query = query;
            workspace.requestListRefresh(true);
        }, () -> {}, workspace::clearFocusedElement);
        search.setBounds(x, y, width, GUIStyle.CONTROL_HEIGHT);
        search.tooltipProperty().set(ignored -> TTooltip.of(MsgUtils.tr("search_hint")), WorkspaceRulesPageElement.class);
        sidebar.add(search);
        y += 25;

        if (!ClientRuleConfigurations.ready()) model.modifiedOnly = false;
        var modifiedFilter = new TCheckboxWidget(model.modifiedOnly);
        modifiedFilter.setBounds(x, y, GUIStyle.CONTROL_HEIGHT, GUIStyle.CONTROL_HEIGHT);
        modifiedFilter.enabledProperty().set(ClientRuleConfigurations.ready() && !page.isVanilla(), WorkspaceRulesPageElement.class);
        modifiedFilter.checkedProperty().addChangeListener((property, previous, checked) -> {
            model.modifiedOnly = checked;
            workspace.requestListRefresh(true);
        });
        Component modifiedHint = MsgUtils.tr("modified_only_hint").copy().append("\n")
            .append(MsgUtils.tr("modified_only_server_required").copy().withStyle(ChatFormatting.GOLD));
        modifiedFilter.tooltipProperty().set(ignored -> TTooltip.of(modifiedHint), WorkspaceRulesPageElement.class);
        sidebar.add(modifiedFilter);
        var modifiedLabel = WorkspaceStyle.label(sidebar, MsgUtils.tr("modified_only"), x + 27, y + 6,
            width - 27, 12, GUIStyle.TEXT_COLOR);
        modifiedLabel.tooltipProperty().set(ignored -> TTooltip.of(modifiedHint), WorkspaceRulesPageElement.class);
        y += 27;

        var initialDifference = new TCheckboxWidget(model.initialDifferenceOnly);
        initialDifference.setBounds(x, y, GUIStyle.CONTROL_HEIGHT, GUIStyle.CONTROL_HEIGHT);
        initialDifference.checkedProperty().addChangeListener((property, previous, checked) -> {
            model.initialDifferenceOnly = checked;
            workspace.requestListRefresh(true);
        });
        Component initialDifferenceHint = MsgUtils.tr("initial_difference_hint");
        initialDifference.tooltipProperty().set(ignored -> TTooltip.of(initialDifferenceHint), WorkspaceRulesPageElement.class);
        sidebar.add(initialDifference);
        var initialDifferenceLabel = WorkspaceStyle.label(sidebar, MsgUtils.tr("initial_difference_only"),
            x + 27, y + 6, width - 27, 12, GUIStyle.TEXT_COLOR);
        initialDifferenceLabel.tooltipProperty().set(ignored -> TTooltip.of(initialDifferenceHint), WorkspaceRulesPageElement.class);
        y += 27;

        var favoritesOnly = new TCheckboxWidget(model.favoritesOnly);
        favoritesOnly.setBounds(x, y, GUIStyle.CONTROL_HEIGHT, GUIStyle.CONTROL_HEIGHT);
        favoritesOnly.checkedProperty().addChangeListener((property, previous, checked) -> {
            model.favoritesOnly = checked;
            workspace.requestListRefresh(true);
        });
        Component favoritesHint = MsgUtils.tr("favorites_only_hint");
        favoritesOnly.tooltipProperty().set(ignored -> TTooltip.of(favoritesHint), WorkspaceRulesPageElement.class);
        sidebar.add(favoritesOnly);
        var favoritesLabel = WorkspaceStyle.label(sidebar, MsgUtils.tr("favorites_only"), x + 27, y + 6,
            width - 27, 12, GUIStyle.TEXT_COLOR);
        favoritesLabel.tooltipProperty().set(ignored -> TTooltip.of(favoritesHint), WorkspaceRulesPageElement.class);
        y += 27;

        if (!ClientRuleConfigurations.ready()) model.savedDefaultOnly = false;
        var savedDefaultFilter = new TCheckboxWidget(model.savedDefaultOnly);
        savedDefaultFilter.setBounds(x, y, GUIStyle.CONTROL_HEIGHT, GUIStyle.CONTROL_HEIGHT);
        savedDefaultFilter.enabledProperty().set(ClientRuleConfigurations.ready() && !page.isVanilla(), WorkspaceRulesPageElement.class);
        savedDefaultFilter.checkedProperty().addChangeListener((property, previous, checked) -> {
            model.savedDefaultOnly = checked;
            workspace.requestListRefresh(true);
        });
        Component savedDefaultHint = MsgUtils.tr("saved_default_only_hint").copy().append("\n")
            .append(MsgUtils.tr("modified_only_server_required").copy().withStyle(ChatFormatting.GOLD));
        savedDefaultFilter.tooltipProperty().set(ignored -> TTooltip.of(savedDefaultHint), WorkspaceRulesPageElement.class);
        sidebar.add(savedDefaultFilter);
        var savedDefaultLabel = WorkspaceStyle.label(sidebar, MsgUtils.tr("saved_default_only"), x + 27, y + 6,
            width - 27, 12, GUIStyle.TEXT_COLOR);
        savedDefaultLabel.tooltipProperty().set(ignored -> TTooltip.of(savedDefaultHint), WorkspaceRulesPageElement.class);
        y += 27;

        addIconDropdown(sidebar, x, y, width, WorkspaceIcon.Kind.SORT, MsgUtils.tr("sort"),
            options(RuleBrowserModel.Sort.values(), value -> MsgUtils.tr("sort." + value.name().toLowerCase(Locale.ROOT))),
            model.sort, sort -> {
                model.sort = sort;
                workspace.requestListRefresh(true);
            });
        y += 27;
        addIconDropdown(sidebar, x, y, width, WorkspaceIcon.Kind.GROUP, MsgUtils.tr("grouping"),
            options(RuleBrowserModel.Grouping.values(), value -> MsgUtils.tr("grouping." + value.name().toLowerCase(Locale.ROOT))),
            model.grouping, grouping -> {
                model.grouping = grouping;
                workspace.requestListRefresh(true);
            });
        y += 27;
        addIconDropdown(sidebar, x, y, width, WorkspaceIcon.Kind.DISTANCE, MsgUtils.tr("distance"),
            options(RuleBrowserModel.Distance.values(), value -> MsgUtils.tr("distance." + value.name().toLowerCase(Locale.ROOT))),
            model.distance, value -> model.distance = value);
        y += 27;
        addIconDropdown(sidebar, x, y, width, WorkspaceIcon.Kind.TIME, MsgUtils.tr("time"),
            options(RuleBrowserModel.Time.values(), value -> MsgUtils.tr("time." + value.name().toLowerCase(Locale.ROOT))),
            model.time, value -> model.time = value);
        y += 29;

        int gap = 4;
        int expandWidth = Math.max(1, (width - gap) / 2);
        int collapseWidth = Math.max(1, width - gap - expandWidth);
        var expand = new ControlButton(Component.translatable("carpet-gui.expand_all"), () -> workspace.expandAll(true));
        expand.setBounds(x, y, expandWidth, GUIStyle.CONTROL_HEIGHT);
        expand.getLabel().textScaleProperty().set(textScale(expand, expandWidth), WorkspaceRulesPageElement.class);
        sidebar.add(expand);
        var collapse = new ControlButton(Component.translatable("carpet-gui.collapse_all"), () -> workspace.expandAll(false));
        collapse.setBounds(x + expandWidth + gap, y, collapseWidth, GUIStyle.CONTROL_HEIGHT);
        collapse.getLabel().textScaleProperty().set(textScale(collapse, collapseWidth), WorkspaceRulesPageElement.class);
        sidebar.add(collapse);

        workspace.registerRuleFilters(modifiedFilter, savedDefaultFilter);
    }

    private double textScale(ControlButton button, int width) {
        return Math.min(GUIStyle.TEXT_SCALE,
            Math.max(1, width - 8) / (double) Math.max(1, getClient().font.width(button.getLabel().getText())));
    }

    private static <T> List<WorkspaceStyle.Option<T>> options(T[] values, Function<T, Component> label) {
        return Arrays.stream(values).map(value -> new WorkspaceStyle.Option<>(value, label.apply(value))).toList();
    }

    private <T> void addDropdown(TElement parent, int x, int y, int width,
                                 List<WorkspaceStyle.Option<T>> options, T selected, Consumer<T> changed) {
        addDropdown(parent, x, y, width, options, selected, changed, null);
    }

    private <T> void addIconDropdown(TElement parent, int x, int y, int width, WorkspaceIcon.Kind kind,
                                     Component hint, List<WorkspaceStyle.Option<T>> options,
                                     T selected, Consumer<T> changed) {
        var icon = new WorkspaceIcon(kind);
        icon.setBounds(x, y, 20, 20);
        icon.hoverableProperty().set(true, WorkspaceRulesPageElement.class);
        icon.tooltipProperty().set(ignored -> TTooltip.of(hint), WorkspaceRulesPageElement.class);
        parent.add(icon);
        addDropdown(parent, x + 27, y, Math.max(1, width - 27), options, selected, changed, hint);
    }

    private <T> void addDropdown(TElement parent, int x, int y, int width,
                                 List<WorkspaceStyle.Option<T>> options, T selected,
                                 Consumer<T> changed, Component hint) {
        var initial = options.stream().filter(option -> option.value().equals(selected)).findFirst().orElse(options.getFirst());
        var dropdown = new WorkspaceStyle.Dropdown<>(initial);
        dropdown.getEntries().addAll(options);
        dropdown.setBounds(x, y, width, GUIStyle.CONTROL_HEIGHT);
        if (hint != null) dropdown.tooltipProperty().set(ignored -> TTooltip.of(hint), WorkspaceRulesPageElement.class);
        dropdown.selectedEntryProperty().addChangeListener((property, previous, current) -> {
            if (current != null) changed.accept(current.value());
        });
        parent.add(dropdown);
    }
}
//#endif

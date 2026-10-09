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

import com.thecsdev.common.math.Point2d;
import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.tooltip.TTooltip;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RulePage;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Grouped rule list; keeps category state and viewport scroll across live updates.
 */
final class RuleListPanel
    extends WorkspacePanel
{
    private final CarpetWorkspaceScreen screen;
    private final RulePage page;
    private final RuleBrowserModel model;
    private final List<WorkspaceRuleRow> rows = new ArrayList<>();
    private List<String> signature = List.of();
    private int ruleCount;
    private int groupCount;

    RuleListPanel(CarpetWorkspaceScreen screen, RulePage page, RuleBrowserModel model) {
        super(10);
        this.screen = screen;
        this.page = page;
        this.model = model;
    }

    @Override
    protected void initCallback() {
        rows.clear();
        var bounds = getBounds();
        int rowWidth = Math.max(1, bounds.width - 20);
        int y = bounds.y + 10;
        var filteredRules = model.rules(page);
        List<RuleBrowserModel.Group> groups = model.groups(filteredRules);
        ruleCount = filteredRules.size();
        groupCount = groups.size();
        signature = model.signature(page);
        if (groups.isEmpty()) {
            WorkspaceStyle.label(
                this, Component.translatable("carpet-gui.empty"), bounds.x + 10, y + 16,
                Math.max(1, bounds.width - 20), 30, WorkspaceStyle.MUTED
            );
        }
        for (var group : groups) {
            Component category = screen.categoryLabel(page, group.category());
            var header = new WorkspaceStyle.CategoryHeader(category, model.expanded(group.category()), () -> {
                model.setExpanded(group.category(), !model.expanded(group.category()));
                screen.requestListRefresh();
            });
            header.setBounds(bounds.x + 10, y, rowWidth, 20);
            header.tooltipProperty().set(ignored -> TTooltip.of(category.copy().append(" (" + group.rules().size() + ")")), RuleListPanel.class);
            add(header);
            y += 24;
            if (model.expanded(group.category())) {
                for (var rule : group.rules()) {
                    String ownerId = page instanceof AllRulesPage allRules ? allRules.ownerId(rule) : page.id();
                    var row = new WorkspaceRuleRow(ownerId, rule, result -> screen.feedback(rule, result), this::cancelDrafts);
                    row.deferOffscreenControls();
                    row.setBounds(bounds.x + 10, y, rowWidth, WorkspaceStyle.RULE_HEIGHT);
                    add(row);
                    rows.add(row);
                    y += WorkspaceStyle.RULE_HEIGHT + 3;
                }
            }
            y += 7;
        }
    }

    boolean interacting() {
        return rows.stream()
            .anyMatch(WorkspaceRuleRow::interacting);
    }

    void beforeMousePress(double x, double y) {
        rows.forEach(WorkspaceRuleRow::clearPendingSave);
        var bounds = getBounds();
        if (x < bounds.x || x >= bounds.endX || y < bounds.y || y >= bounds.endY) return;
        for (WorkspaceRuleRow target : rows) {
            if (target.resetHit(x, y)) {
                cancelDrafts(target.stateId());
                return;
            }
            if (target.saveHit(x, y)) {
                // A multi-category rule can have several rows; save the focused row's draft only once.
                WorkspaceRuleRow draft = rows.stream().filter(row -> row.stateId().equals(target.stateId()) && row.focusedText())
                    .findFirst().orElse(target);
                target.prepareSave(draft.draftValue());
                if (draft.focusedText()) draft.suppressNextBlur();
                return;
            }
        }
    }

    private void cancelDrafts(String stateId) {
        rows.stream().filter(row -> row.stateId().equals(stateId)).forEach(WorkspaceRuleRow::cancelDraft);
    }

    void cancelDrafts() {
        rows.forEach(WorkspaceRuleRow::cancelDraft);
    }

    void refreshRows() {
        rows.forEach(WorkspaceRuleRow::refresh);
    }

    boolean changed() {
        return !signature.equals(model.signature(page));
    }

    Component countsText() {
        return Component.translatable("carpet-gui.counts", ruleCount, groupCount);
    }

    void rebuild(boolean resetScroll) {
        Point2d previous = resetScroll ? Point2d.ZERO : scrollAmountProperty().get();
        cancelDrafts();
        if (findChild(TElement::isFocused, true).isPresent()) {
            screen.focusedElementProperty().set(null, RuleListPanel.class);
        }
        scrollAmountProperty().set(Point2d.ZERO, RuleListPanel.class);
        clearAndInit();
        scrollAmountProperty().set(previous, RuleListPanel.class);
    }
}
//#endif

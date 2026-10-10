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
import com.thecsdev.commonmc.api.client.gui.panel.TPanelElement;
import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
import com.thecsdev.commonmc.api.client.gui.tooltip.TTooltip;
import io.github.piscescup.fabricmc.carpetgui.api.EditableRuleView;
import io.github.piscescup.fabricmc.carpetgui.gui.CarpetWorkspaceScreen;
import io.github.piscescup.fabricmc.carpetgui.api.PersistentRuleEditor;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleEditResult;
import io.github.piscescup.fabricmc.carpetgui.gui.pages.RulePage;
import io.github.piscescup.fabricmc.carpetgui.api.RuleView;
import io.github.piscescup.fabricmc.carpetgui.gui.GUIStyle;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.button.Button;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.NativeTextInput;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.button.ControlButton;
import io.github.piscescup.fabricmc.carpetgui.network.ClientRuleChanges;
import io.github.piscescup.fabricmc.carpetgui.network.RuleChangeEvent;
import io.github.piscescup.fabricmc.carpetgui.store.RuleGroupStore;
import net.minecraft.network.chat.Component;

import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Group page first; Git-like status/stage/commit/push is a separate workflow over its member rules. */
final class RuleGroupWorkspace {
    static final String ID = CarpetWorkspaceEditor.RULE_GROUPS_PAGE_ID;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CarpetWorkspaceScreen screen;
    private final List<? extends RulePage> pages;
    private final List<WorkspaceRuleRow> activeRows = new ArrayList<>();
    private final Map<WorkspaceRuleRow, String> activeRowKeys = new LinkedHashMap<>();
    private final Map<String, String> draftSnapshot = new LinkedHashMap<>();
    private String selectedGroupId;
    private Mode mode = Mode.VIEW;
    private boolean creating;
    private String nameDraft = "";
    private String tagDraft = "";
    private String manageSearchDraft = "";
    private String commitMessageDraft = "";
    private long observedRemoteRevision;
    private List<WorkspacePanel> panels = List.of();
    private WorkspacePanel managePanel;
    private List<MembershipRow> membershipRows = List.of();
    private TLabelElement manageEmpty;
    private int manageRowsY;
    private int[] pendingScrollOffsets;

    RuleGroupWorkspace(CarpetWorkspaceScreen screen, List<? extends RulePage> pages) {
        this.screen = screen;
        this.pages = pages;
    }

    void init(int x, int y, int width, int height) {
        observedRemoteRevision = ClientRuleChanges.revision();
        activeRows.clear();
        activeRowKeys.clear();
        managePanel = null;
        membershipRows = List.of();
        manageEmpty = null;
        List<RuleGroupStore.Group> groups = RuleGroupStore.groups();
        if (selectedGroupId == null || RuleGroupStore.find(selectedGroupId) == null) {
            selectedGroupId = groups.isEmpty() ? null : groups.getFirst().id();
        }
        RuleGroupStore.Group selected = selectedGroupId == null ? null : RuleGroupStore.find(selectedGroupId);
        int actionsHeight = 34;
        int contentHeight = Math.max(1, height - actionsHeight);
        // Keep the navigation and history compact so the editable rule list gets most
        // of the available width. The lower bounds still protect small GUI scales.
        int leftWidth = Math.clamp(width * 18 / 100, Math.min(100, width / 3), 175);
        int rightWidth = Math.clamp(width * 24 / 100, Math.min(130, width / 3), 250);
        int centerWidth = Math.max(1, width - leftWidth - rightWidth);
        var left = new WorkspacePanel(8);
        var center = new WorkspacePanel(8);
        var right = new WorkspacePanel(8);
        panels = List.of(left, center, right);
        if (pendingScrollOffsets != null) {
            int[] offsets = pendingScrollOffsets;
            pendingScrollOffsets = null;
            for (int index = 0; index < panels.size(); index++) {
                WorkspacePanel panel = panels.get(index);
                int offset = offsets[index];
                // Restore after the panel's content and scroll extent have initialized.
                panel.eInitialized.addListener(ignored -> panel.scroll(0, -offset));
            }
        }
        screen.addWorkspacePane(left, x, y, leftWidth, contentHeight);
        screen.addWorkspacePane(center, x + leftWidth, y, centerWidth, contentHeight);
        screen.addWorkspacePane(right, x + leftWidth + centerWidth, y, rightWidth, contentHeight);
        initGroups(left, groups);
        switch (mode) {
            case VIEW -> initGroupRules(center, selected);
            case MANAGE -> initManageRules(center, selected);
            case STATUS -> initStatus(center, selected);
            case COMMIT -> initCommit(center, selected);
        }
        initHistory(right, selected);
        initActions(x + leftWidth, y + contentHeight, centerWidth, actionsHeight, selected);
    }

    private void initGroups(WorkspacePanel panel, List<RuleGroupStore.Group> groups) {
        var bounds = panel.getBounds();
        int x = bounds.x + 8, y = bounds.y + 8, width = Math.max(1, bounds.width - 16);
        WorkspaceStyle.label(panel, tr("rule_groups"), x, y, width, 16, GUIStyle.ACCENT_COLOR);
        y += 22;
        var create = new ControlButton(tr(creating ? "cancel" : "new_group"), () -> {
            creating = !creating;
            nameDraft = "";
            tagDraft = "";
            screen.rebuildWorkspace();
        });
        create.setBounds(x, y, width, GUIStyle.CONTROL_HEIGHT);
        panel.add(create);
        y += 27;
        if (creating) {
            var name = new NativeTextInput(tr("group_name"), nameDraft, value -> nameDraft = value, () -> {}, this::cancelCreate);
            name.setBounds(x, y, width, GUIStyle.CONTROL_HEIGHT);
            panel.add(name);
            y += 24;
            var tag = new NativeTextInput(tr("group_tag"), tagDraft, value -> tagDraft = value, () -> {}, this::cancelCreate);
            tag.setBounds(x, y, width, GUIStyle.CONTROL_HEIGHT);
            panel.add(tag);
            y += 24;
            var confirm = new ControlButton(tr("create_group"), this::createGroup);
            confirm.setBounds(x, y, width, GUIStyle.CONTROL_HEIGHT);
            panel.add(confirm);
            y += 29;
        }
        if (groups.isEmpty()) {
            paragraph(panel, tr("no_groups"), x, y + 3, width, GUIStyle.MUTED_COLOR);
            return;
        }
        for (RuleGroupStore.Group group : groups) {
            var item = new Button(Component.empty(), () -> {
                cancelDrafts();
                selectedGroupId = group.id();
                mode = Mode.VIEW;
                screen.rebuildWorkspace();
            });
            item.setSelected(group.id().equals(selectedGroupId));
            item.setBounds(x, y, width, GUIStyle.RULE_HEIGHT);
            panel.add(item);
            WorkspaceStyle.label(panel, Component.literal(group.name()), x + 7, y + 3,
                Math.max(1, width - 14), 10,
                group.id().equals(selectedGroupId) ? GUIStyle.ACCENT_COLOR : GUIStyle.TEXT_COLOR
            );
            String state = group.commitsAhead() > 0 ? "↑" + group.commitsAhead() : "✓";
            var label = WorkspaceStyle.label(panel,
                Component.literal((group.tag().isBlank() ? "HEAD" : group.tag()) + "  ·  r" + group.revision() + "  " + state),
                x + 7, y + 14, Math.max(1, width - 14), 9, GUIStyle.MUTED_COLOR
            );
            label.textScaleProperty().set(GUIStyle.SMALL_TEXT_SCALE, RuleGroupWorkspace.class);
            label.wrapTextProperty().set(false, RuleGroupWorkspace.class);
            y += GUIStyle.RULE_HEIGHT + 3;
        }
    }

    private void cancelCreate() {
        creating = false;
        nameDraft = "";
        tagDraft = "";
        screen.rebuildWorkspace();
    }

    private void createGroup() {
        String name = nameDraft.strip();
        if (name.isEmpty()) {
            screen.showFeedback(tr("group_name_required"));
            return;
        }
        RuleGroupStore.Group group = RuleGroupStore.create(name, tagDraft);
        selectedGroupId = group.id();
        creating = false;
        nameDraft = "";
        tagDraft = "";
        mode = Mode.VIEW;
        screen.showFeedback(Component.translatable("carpet-gui.workspace.group_created", group.name()));
        screen.rebuildWorkspace();
    }

    /** The normal group page: only member rules, using the same live editors as ordinary rule pages. */
    private void initGroupRules(WorkspacePanel panel, RuleGroupStore.Group group) {
        var bounds = panel.getBounds();
        int x = bounds.x + 9, y = bounds.y + 8, width = Math.max(1, bounds.width - 18);
        if (group == null) {
            WorkspaceStyle.label(panel, tr("group_rules"), x, y, width, 16, GUIStyle.ACCENT_COLOR);
            paragraph(panel, tr("select_or_create_group"), x, y + 24, width, GUIStyle.MUTED_COLOR);
            return;
        }
        WorkspaceStyle.label(panel, Component.literal(group.name()), x, y, Math.max(1, width - 100), 16, GUIStyle.ACCENT_COLOR);
        var manage = new ControlButton(tr("manage_rules"), () -> setMode(Mode.MANAGE));
        manage.setBounds(x + width - 94, y - 3, 94, GUIStyle.CONTROL_HEIGHT);
        panel.add(manage);
        y += 24;
        y = addStatusLegend(panel, x, y, width) + 6;
        Map<String, RuleRef> byId = rulesById();
        if (group.members().isEmpty()) {
            y = paragraph(panel, tr("empty_group"), x, y + 8, width, GUIStyle.MUTED_COLOR) + 12;
            var addRules = new ControlButton(tr("add_group_rules"), () -> setMode(Mode.MANAGE));
            addRules.setBounds(x, y, Math.min(150, width), GUIStyle.CONTROL_HEIGHT);
            panel.add(addRules);
            return;
        }
        for (String ruleId : group.members()) {
            RuleRef ref = byId.get(ruleId);
            if (ref == null) {
                addMembershipRow(panel, null, ruleId, true, group, x, y, width);
                y += GUIStyle.RULE_HEIGHT + 3;
                continue;
            }
            var row = new WorkspaceRuleRow(ref.ownerId(), ref.rule(), result -> screen.feedback(ref.rule(), result),
                this::cancelDrafts, () -> ruleColor(group, ruleId, ref.rule()),
                () -> ruleDetail(group, ruleId, ref.rule()));
            row.setBounds(x, y, width, GUIStyle.RULE_HEIGHT);
            panel.add(row);
            activeRows.add(row);
            activeRowKeys.put(row, ruleId);
            y += GUIStyle.RULE_HEIGHT + 3;
        }
    }

    /** Membership picker. Adding a member does not stage or commit it. */
    private void initManageRules(WorkspacePanel panel, RuleGroupStore.Group group) {
        var bounds = panel.getBounds();
        int x = bounds.x + 9, y = bounds.y + 8, width = Math.max(1, bounds.width - 18);
        int doneWidth = Math.min(64, width);
        int searchWidth = Math.min(Math.max(1, width - doneWidth - 8), Math.clamp(width / 3, 70, 150));
        int searchX = x + width - doneWidth - 6 - searchWidth;
        WorkspaceStyle.label(panel, tr("manage_group_rules"), x, y, Math.max(1, searchX - x - 5), 16, GUIStyle.ACCENT_COLOR);
        var search = new NativeTextInput(tr("search"), manageSearchDraft, value -> {
            manageSearchDraft = value;
            filterMembershipRows();
        }, () -> {}, () -> screen.focusedElementProperty().set(null, RuleGroupWorkspace.class));
        search.setBounds(searchX, y - 3, searchWidth, GUIStyle.CONTROL_HEIGHT);
        search.tooltipProperty().set(ignored -> TTooltip.of(tr("search_hint")), RuleGroupWorkspace.class);
        panel.add(search);
        var done = new ControlButton(tr("done"), () -> setMode(Mode.VIEW));
        done.setBounds(x + width - doneWidth, y - 3, doneWidth, GUIStyle.CONTROL_HEIGHT);
        panel.add(done);
        y += 25;
        if (group == null) {
            paragraph(panel, tr("select_or_create_group"), x, y, width, GUIStyle.MUTED_COLOR);
            return;
        }
        List<RuleRef> rules = rules();
        List<MembershipRow> rows = new ArrayList<>(rules.size());
        managePanel = panel;
        manageRowsY = y;
        for (RuleRef ref : rules) {
            WorkspaceStaticPanel row = addMembershipRow(panel, ref, ref.key(), group.members().contains(ref.key()), group, x, y, width);
            rows.add(new MembershipRow(ref, row));
            y += 29;
        }
        membershipRows = List.copyOf(rows);
        manageEmpty = WorkspaceStyle.label(panel, Component.empty(), x, manageRowsY, width, 15, GUIStyle.MUTED_COLOR);
        manageEmpty.wrapTextProperty().set(true, RuleGroupWorkspace.class);
        filterMembershipRows();
    }

    private WorkspaceStaticPanel addMembershipRow(TElement parent, RuleRef ref, String key, boolean member,
                                                   RuleGroupStore.Group group, int x, int y, int width) {
        var row = new WorkspaceStaticPanel(GUIStyle.RULE_BACKGROUND_COLOR, GUIStyle.BORDER_COLOR, GUIStyle.BORDER_COLOR);
        row.setBounds(x, y, width, 26);
        parent.add(row);
        int actionWidth = Math.clamp(width / 5, 46, 70);
        WorkspaceStyle.label(row, Component.literal(member ? "●" : "+"), x + 6, y + 7, 12, 11,
            member ? GUIStyle.ACCENT_COLOR : GUIStyle.MUTED_COLOR
        );
        Component title = ref == null ? Component.literal(key) : ref.rule().label();
        String detail = ref == null ? tr("missing_rule").getString()
            : ref.owner().getString() + "  ·  " + ref.rule().value();
        int titleColor = member && ref != null ? ruleColor(group, key, ref.rule()) : GUIStyle.TEXT_COLOR;
        WorkspaceStyle.label(row, title, x + 21, y + 3, Math.max(1, width - actionWidth - 29), 10, titleColor);
        var detailLabel = WorkspaceStyle.label(row, Component.literal(detail), x + 21, y + 14,
            Math.max(1, width - actionWidth - 29), 9, GUIStyle.MUTED_COLOR
        );
        detailLabel.textScaleProperty().set(GUIStyle.SMALL_TEXT_SCALE, RuleGroupWorkspace.class);
        detailLabel.wrapTextProperty().set(false, RuleGroupWorkspace.class);
        Runnable action = member
            ? () -> mutate(() -> RuleGroupStore.removeMember(group, key))
            : () -> mutate(() -> RuleGroupStore.addMember(group, key));
        var button = new Button(tr(member ? "remove" : "add_rule"), action);
        button.setBounds(x + width - actionWidth - 3, y + 3, actionWidth, 20);
        row.add(button);
        return row;
    }

    private void filterMembershipRows() {
        if (managePanel == null) return;
        managePanel.scrollToTop();
        int y = manageRowsY;
        int visibleRows = 0;
        for (MembershipRow entry : membershipRows) {
            boolean visible = matchesManageSearch(entry.ref());
            entry.row().visibleProperty().set(visible, RuleGroupWorkspace.class);
            int targetY = visible ? y : manageRowsY;
            int deltaY = targetY - entry.row().getBounds().y;
            entry.row().move(0, deltaY);
            if (!visible) continue;
            y += 29;
            visibleRows++;
        }
        if (manageEmpty != null) {
            manageEmpty.setText(tr(membershipRows.isEmpty() ? "no_rules" : "no_matching_rules"));
            manageEmpty.setBounds(manageEmpty.getBounds().x, manageRowsY, manageEmpty.getBounds().width, 15);
            manageEmpty.visibleProperty().set(visibleRows == 0, RuleGroupWorkspace.class);
        }
        managePanel.refreshScrollExtent();
    }

    private boolean matchesManageSearch(RuleRef ref) {
        String query = normalizeSearch(manageSearchDraft);
        if (query.isEmpty()) return true;
        StringBuilder text = new StringBuilder(ref.owner().getString())
            .append(' ').append(ref.ownerId())
            .append(' ').append(ref.key())
            .append(' ').append(ref.rule().value());
        ref.rule().searchTerms().forEach(term -> text.append(' ').append(term));
        String searchable = normalizeSearch(text.toString());
        for (String token : query.split("\\s+")) {
            if (!searchable.contains(token)) return false;
        }
        return true;
    }

    private static String normalizeSearch(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKD)
            .replaceAll("\\p{M}+", "")
            .toLowerCase(Locale.ROOT)
            .strip();
    }

    /** Status GUI: staged and unstaged are deliberately distinct. */
    private void initStatus(WorkspacePanel panel, RuleGroupStore.Group group) {
        var bounds = panel.getBounds();
        int x = bounds.x + 9, y = bounds.y + 8, width = Math.max(1, bounds.width - 18);
        WorkspaceStyle.label(panel, tr("status_title"), x, y, Math.max(1, width - 70), 16, GUIStyle.ACCENT_COLOR);
        var back = new ControlButton(tr("back"), () -> setMode(Mode.VIEW));
        back.setBounds(x + width - 64, y - 3, 64, GUIStyle.CONTROL_HEIGHT);
        panel.add(back);
        y += 25;
        if (group == null) {
            paragraph(panel, tr("select_or_create_group"), x, y, width, GUIStyle.MUTED_COLOR);
            return;
        }
        Map<String, RuleRef> byId = rulesById();
        List<RuleChange> unstaged = changes(group, byId);
        WorkspaceStyle.label(panel, Component.translatable("carpet-gui.workspace.unstaged_changes", unstaged.size()),
            x, y, width, 15, GUIStyle.ACCENT_COLOR
        );
        y += 20;
        if (unstaged.isEmpty()) y = paragraph(panel, tr("no_unstaged_changes"), x + 8, y, width - 8, GUIStyle.MUTED_COLOR) + 7;
        for (RuleChange change : unstaged) {
            addChangeRow(panel, change.ref(), change.key(), change.detail(), change.marker(), change.color(), "stage",
                () -> mutate(() -> RuleGroupStore.stage(group, change.key(), change.value())), x, y, width);
            y += 29;
        }
        y += 5;
        WorkspaceStyle.label(panel, Component.translatable("carpet-gui.workspace.staged_changes", group.staged().size()),
            x, y, width, 15, GUIStyle.ACCENT_COLOR
        );
        y += 20;
        if (group.staged().isEmpty()) y = paragraph(panel, tr("no_staged_changes"), x + 8, y, width - 8, GUIStyle.MUTED_COLOR) + 7;
        for (Map.Entry<String, String> staged : group.staged().entrySet()) {
            addChangeRow(panel, byId.get(staged.getKey()), staged.getKey(), staged.getValue(), "A", GUIStyle.STAGED_RULE_COLOR, "unstage",
                () -> mutate(() -> RuleGroupStore.unstage(group, staged.getKey())), x, y, width);
            y += 29;
        }
        y += 8;
        var restore = new ControlButton(tr("restore_head"), () -> restore(group));
        restore.setBounds(x, y, Math.min(145, width), GUIStyle.CONTROL_HEIGHT);
        panel.add(restore);
    }

    private void initCommit(WorkspacePanel panel, RuleGroupStore.Group group) {
        var bounds = panel.getBounds();
        int x = bounds.x + 9, y = bounds.y + 8, width = Math.max(1, bounds.width - 18);
        WorkspaceStyle.label(panel, tr("create_commit"), x, y, Math.max(1, width - 70), 16, GUIStyle.ACCENT_COLOR);
        var back = new ControlButton(tr("back"), () -> setMode(Mode.STATUS));
        back.setBounds(x + width - 64, y - 3, 64, GUIStyle.CONTROL_HEIGHT);
        panel.add(back);
        y += 28;
        if (group == null) {
            paragraph(panel, tr("select_or_create_group"), x, y, width, GUIStyle.MUTED_COLOR);
            return;
        }
        WorkspaceStyle.label(panel, tr("commit_message"), x, y, width, 13, GUIStyle.TEXT_COLOR);
        y += 17;
        var message = new NativeTextInput(tr("commit_message_hint"), commitMessageDraft,
            value -> commitMessageDraft = value, () -> {}, () -> setMode(Mode.STATUS));
        message.setBounds(x, y, width, GUIStyle.CONTROL_HEIGHT);
        panel.add(message);
        y += 29;
        var commit = new ControlButton(Component.translatable("carpet-gui.workspace.commit_count", group.staged().size()),
            () -> commitNow(group));
        commit.setBounds(x, y, Math.min(170, width), GUIStyle.CONTROL_HEIGHT);
        commit.enabledProperty().set(!group.staged().isEmpty(), RuleGroupWorkspace.class);
        panel.add(commit);
        y += 31;
        WorkspaceStyle.label(panel, Component.translatable("carpet-gui.workspace.staged_changes", group.staged().size()),
            x, y, width, 15, GUIStyle.ACCENT_COLOR
        );
        y += 20;
        Map<String, RuleRef> byId = rulesById();
        for (Map.Entry<String, String> staged : group.staged().entrySet()) {
            addChangeRow(panel, byId.get(staged.getKey()), staged.getKey(), staged.getValue(), "A", GUIStyle.STAGED_RULE_COLOR,
                null, null, x, y, width);
            y += 29;
        }
        if (group.staged().isEmpty()) paragraph(panel, tr("nothing_to_commit"), x + 8, y, width - 8, GUIStyle.MUTED_COLOR);
    }

    private void addChangeRow(TElement parent, RuleRef ref, String key, String value, String marker, int color,
                              String actionKey, Runnable action, int x, int y, int width) {
        var row = new WorkspaceStaticPanel(GUIStyle.RULE_BACKGROUND_COLOR, GUIStyle.BORDER_COLOR, GUIStyle.BORDER_COLOR);
        row.setBounds(x, y, width, 26);
        parent.add(row);
        int actionWidth = action == null ? 0 : Math.clamp(width / 5, 46, 70);
        WorkspaceStyle.label(row, Component.literal(marker), x + 6, y + 7, 12, 11, color);
        Component title = ref == null ? Component.literal(key) : ref.rule().label();
        String owner = ref == null ? tr("missing_rule").getString() : ref.owner().getString();
        WorkspaceStyle.label(row, title, x + 21, y + 3, Math.max(1, width - actionWidth - 29), 10, color);
        var detail = WorkspaceStyle.label(row, Component.literal(owner + "  ·  " + value), x + 21, y + 14,
            Math.max(1, width - actionWidth - 29), 9, GUIStyle.MUTED_COLOR
        );
        detail.textScaleProperty().set(GUIStyle.SMALL_TEXT_SCALE, RuleGroupWorkspace.class);
        detail.wrapTextProperty().set(false, RuleGroupWorkspace.class);
        if (action != null) {
            var button = new Button(tr(actionKey), action);
            button.setBounds(x + width - actionWidth - 3, y + 3, actionWidth, 20);
            row.add(button);
        }
    }

    private void initActions(int x, int y, int width, int height, RuleGroupStore.Group group) {
        var bar = new WorkspaceStaticPanel(0x55000000, GUIStyle.BORDER_COLOR, GUIStyle.BORDER_COLOR);
        bar.setBounds(x, y, width, Math.max(1, height - 4));
        screen.addWorkspaceElement(bar);
        int gap = 4;
        int inset = 7;
        int available = Math.max(1, width - inset * 2 - gap * 3);
        int buttonWidth = Math.max(1, available / 4);
        addAction(bar, x + inset, y + 5, buttonWidth, "status", mode == Mode.STATUS, () -> setMode(Mode.STATUS), group != null);
        addAction(bar, x + inset + (buttonWidth + gap), y + 5, buttonWidth, "add", false, () -> stageAll(group), group != null);
        addAction(bar, x + inset + (buttonWidth + gap) * 2, y + 5, buttonWidth, "commit", mode == Mode.COMMIT,
            () -> setMode(Mode.COMMIT), group != null && !group.staged().isEmpty());
        addAction(bar, x + inset + (buttonWidth + gap) * 3, y + 5,
            available - (buttonWidth + gap) * 3, "push", false, () -> push(group), group != null && group.commitsAhead() > 0);
    }

    private void addAction(TElement parent, int x, int y, int width, String key, boolean selected, Runnable action, boolean enabled) {
        var button = new Button(tr(key), action);
        button.setSelected(selected);
        button.enabledProperty().set(enabled, RuleGroupWorkspace.class);
        button.setBounds(x, y, width, GUIStyle.CONTROL_HEIGHT);
        parent.add(button);
    }

    /** Right-hand Git log: graph line, commit message, refs, time and pushed state. */
    private void initHistory(WorkspacePanel panel, RuleGroupStore.Group group) {
        var bounds = panel.getBounds();
        int x = bounds.x + 9, y = bounds.y + 8, width = Math.max(1, bounds.width - 18);
        WorkspaceStyle.label(panel, tr("process"), x, y, width, 16, GUIStyle.ACCENT_COLOR);
        y += 23;
        if (group == null) {
            paragraph(panel, tr("no_group_selected"), x, y, width, GUIStyle.MUTED_COLOR);
            return;
        }
        WorkspaceStyle.label(panel, Component.literal(group.name()), x, y, width, 16, GUIStyle.TEXT_COLOR);
        y += 19;
        Component summary = Component.translatable("carpet-gui.workspace.pipeline_summary", group.members().size(),
            group.staged().size(), group.commitsAhead());
        y = paragraph(panel, summary, x, y, width, GUIStyle.MUTED_COLOR) + 14;
        if (group.history().isEmpty()) {
            paragraph(panel, tr("no_commits_graph"), x, y, width, GUIStyle.MUTED_COLOR);
            return;
        }
        List<RuleGroupStore.Commit> history = group.history();
        for (int index = 0; index < history.size(); index++) {
            RuleGroupStore.Commit commit = history.get(index);
            boolean pending = commit.revision() > group.pushedRevision();
            if (index == 0) {
                var selected = new TPanelElement.Paintable(0xAA1F3156, 0, 0);
                selected.setBounds(x - 4, y - 3, width + 8, 37);
                panel.add(selected);
            }
            var graph = new CommitGraphNode(pending, index < history.size() - 1);
            graph.setBounds(x, y, 13, 38);
            panel.add(graph);
            WorkspaceStyle.label(panel, Component.literal(commit.message()), x + 18, y,
                Math.max(1, width - 37), 12, GUIStyle.TEXT_COLOR
            ).wrapTextProperty().set(false, RuleGroupWorkspace.class);
            String refs = "r" + commit.revision();
            if (index == 0 && !group.tag().isBlank()) refs += "  ◇ " + group.tag();
            refs += pending ? "  local" : "  server";
            var metadata = WorkspaceStyle.label(panel, Component.literal(refs), x + 18, y + 14,
                Math.max(1, width - 37), 9, pending ? GUIStyle.FOCUS_COLOR : GUIStyle.MUTED_COLOR
            );
            metadata.textScaleProperty()
                .set(GUIStyle.SMALL_TEXT_SCALE, RuleGroupWorkspace.class);
            String time = TIME.format(
                Instant.ofEpochMilli(commit.time()).atZone(ZoneId.systemDefault())
            );
            var date = WorkspaceStyle.label(
                panel,
                Component.literal(time),
                x + 18, y + 24, Math.max(1, width - 37), 9,
                GUIStyle.MUTED_COLOR
            );
            date.textScaleProperty()
                .set(GUIStyle.SMALL_TEXT_SCALE, RuleGroupWorkspace.class);
            WorkspaceStyle.label(
                panel,
                Component.literal(pending ? "↑" : "✓"),
                x + width - 16, y + 3, 14, 12,
                pending ? GUIStyle.ACCENT_COLOR : 0xFF55FF55
            );
            y += 39;
        }
    }

    private List<RuleChange> changes(RuleGroupStore.Group group, Map<String, RuleRef> byId) {
        List<RuleChange> result = new ArrayList<>();
        for (String key : group.members()) {
            RuleRef ref = byId.get(key);
            if (ref == null) continue;
            String current = draftSnapshot.getOrDefault(key, ref.rule().value());
            String baseline = group.isStaged(key) ?
                group.stagedValue(key) :
                group.headValue(key);

            if (baseline == null || !baseline.equals(current)) {
                boolean isNew = !group.tracked(key) && !group.isStaged(key);
                RuleChangeEvent remote = remoteChange(group, key, ref.rule());
                String detail = remote == null ?
                    current :
                    remote.oldValue() + " → " + remote.newValue() + "  ·  " + remote.actorName();
                result.add(
                    new RuleChange(
                        key, current, detail, ref,
                        remote != null ? "R" : isNew ? "?" : "M",
                        remote != null ? GUIStyle.REMOTE_RULE_COLOR : isNew ? GUIStyle.NEW_RULE_COLOR : GUIStyle.MODIFIED_RULE_COLOR
                    )
                );
            }
        }
        return result;
    }

    private void stageAll(RuleGroupStore.Group group) {
        if (group == null) return;
        Map<String, RuleRef> byId = rulesById();
        Map<String, String> currentValues = new LinkedHashMap<>();
        for (String key : group.members()) {
            RuleRef ref = byId.get(key);
            if (ref != null) currentValues.put(key, draftSnapshot.getOrDefault(key, ref.rule().value()));
        }
        draftSnapshot.clear();
        int count = RuleGroupStore.stageAll(group, currentValues);
        screen.showFeedback(Component.translatable("carpet-gui.workspace.add_result", count));
        mode = Mode.STATUS;
        screen.rebuildWorkspace();
    }

    private void commitNow(RuleGroupStore.Group group) {
        if (commitMessageDraft.strip().isEmpty()) {
            screen.showFeedback(tr("commit_message_required"));
            return;
        }
        if (!RuleGroupStore.commit(group, commitMessageDraft)) {
            screen.showFeedback(tr("nothing_to_commit"));
            return;
        }
        commitMessageDraft = "";
        mode = Mode.VIEW;
        screen.showFeedback(tr("commit_waiting_push"));
        screen.rebuildWorkspace();
    }

    private void push(RuleGroupStore.Group group) {
        if (group == null || group.commitsAhead() == 0) {
            screen.showFeedback(tr("nothing_to_push"));
            return;
        }
        int accepted = applyHead(group, true);
        int total = group.head().size();
        if (accepted == total) RuleGroupStore.markPushed(group);
        screen.showFeedback(Component.translatable("carpet-gui.workspace.push_result", accepted, total));
        mode = Mode.VIEW;
        screen.refreshRules();
        screen.rebuildWorkspace();
    }

    private void restore(RuleGroupStore.Group group) {
        RuleGroupStore.clearStage(group);
        int accepted = applyHead(group, false);
        screen.showFeedback(Component.translatable("carpet-gui.workspace.restore_result", accepted, group.head().size()));
        mode = Mode.STATUS;
        screen.refreshRules();
        screen.rebuildWorkspace();
    }

    private int applyHead(RuleGroupStore.Group group, boolean persistent) {
        Map<String, RuleRef> byId = rulesById();
        int accepted = 0;
        for (Map.Entry<String, String> entry : group.head().entrySet()) {
            RuleRef ref = byId.get(entry.getKey());
            if (ref == null || !(ref.rule() instanceof EditableRuleView editable)) continue;
            RuleEditResult result;
            if (persistent && editable.editor() instanceof PersistentRuleEditor editor && editor.canSaveDefault()) {
                result = editor.saveDefault(entry.getValue(), ignored -> {});
            } else {
                result = editable.editor().submit(entry.getValue(), ignored -> {});
            }
            if (result.accepted()) accepted++;
        }
        return accepted;
    }

    private void setMode(Mode value) {
        cancelDrafts();
        mode = value;
        screen.rebuildWorkspace();
    }

    private void mutate(Runnable change) {
        pendingScrollOffsets = panels.stream()
            .mapToInt(panel -> Math.max(0, panel.getBounds().y - panel.getContentBounds().y))
            .toArray();
        change.run();
        screen.rebuildWorkspace();
    }

    void tick() {
        long remoteRevision = ClientRuleChanges.revision();
        if (remoteRevision != observedRemoteRevision) {
            observedRemoteRevision = remoteRevision;
            if (mode != Mode.VIEW) {
                screen.rebuildWorkspace();
                return;
            }
        }
        activeRows.forEach(WorkspaceRuleRow::refresh);
    }

    void beforeMousePress(double x, double y) {
        draftSnapshot.clear();
        activeRows.forEach(WorkspaceRuleRow::clearPendingSave);
        activeRows.stream().filter(WorkspaceRuleRow::focusedText)
            .forEach(row -> draftSnapshot.put(activeRowKeys.get(row), row.draftValue()));
        for (WorkspaceRuleRow target : activeRows) {
            if (target.resetHit(x, y)) {
                cancelDrafts(target.stateId());
                return;
            }
            if (target.saveHit(x, y)) {
                WorkspaceRuleRow draft = activeRows.stream()
                    .filter(row -> row.stateId().equals(target.stateId()) && row.focusedText()).findFirst().orElse(target);
                target.prepareSave(draft.draftValue());
                if (draft.focusedText()) draft.suppressNextBlur();
                return;
            }
        }
    }

    void cancelDrafts() {
        activeRows.forEach(WorkspaceRuleRow::cancelDraft);
    }

    private void cancelDrafts(String stateId) {
        activeRows.stream().filter(row -> row.stateId().equals(stateId)).forEach(WorkspaceRuleRow::cancelDraft);
    }

    private int paragraph(TElement parent, Component text, int x, int y, int width, int color) {
        var label = WorkspaceStyle.label(parent, text, x, y, width, 10, color);
        label.wrapTextProperty().set(true, RuleGroupWorkspace.class);
        label.setBoundsToFitText(x, y, Math.max(1, width));
        return label.getBounds().endY;
    }

    private int addStatusLegend(TElement parent, int x, int y, int width) {
        String[] keys = {"color_new", "color_modified", "color_staged", "color_remote", "color_committed"};
        int[] colors = {GUIStyle.NEW_RULE_COLOR, GUIStyle.MODIFIED_RULE_COLOR, GUIStyle.STAGED_RULE_COLOR, GUIStyle.REMOTE_RULE_COLOR, GUIStyle.TEXT_COLOR};
        int nextX = x;
        int bottom = y;
        for (int index = 0; index < keys.length; index++) {
            Component text = Component.literal("● ").append(tr(keys[index]));
            int itemWidth = Math.min(width, (int) Math.ceil(screen.getClient().font.width(text)
                * GUIStyle.SMALL_TEXT_SCALE) + 2);
            if (nextX > x && nextX + itemWidth > x + width) {
                nextX = x;
                y = bottom + 4;
            }
            var label = WorkspaceStyle.label(parent, text, nextX, y, itemWidth, 11, colors[index]);
            label.textScaleProperty().set(GUIStyle.SMALL_TEXT_SCALE, RuleGroupWorkspace.class);
            label.wrapTextProperty().set(true, RuleGroupWorkspace.class);
            label.setBoundsToFitText(nextX, y, Math.max(1, itemWidth));
            bottom = Math.max(bottom, label.getBounds().endY);
            nextX += itemWidth + 10;
        }
        return bottom;
    }

    private int ruleColor(RuleGroupStore.Group group, String key, RuleView rule) {
        String current = rule.value();
        if (group.isStaged(key)) {
            if (current.equals(group.stagedValue(key))) return GUIStyle.STAGED_RULE_COLOR;
        }
        if (remoteChange(group, key, rule) != null) return GUIStyle.REMOTE_RULE_COLOR;
        if (group.isStaged(key)) return GUIStyle.MODIFIED_RULE_COLOR;
        if (!group.tracked(key)) return GUIStyle.NEW_RULE_COLOR;
        return current.equals(group.headValue(key)) ? GUIStyle.TEXT_COLOR : GUIStyle.MODIFIED_RULE_COLOR;
    }

    private Component ruleDetail(RuleGroupStore.Group group, String key, RuleView rule) {
        RuleChangeEvent remote = remoteChange(group, key, rule);
        return remote == null ? rule.description()
            : Component.translatable("carpet-gui.workspace.remote_change", remote.actorName(),
                remote.oldValue(), remote.newValue());
    }

    private RuleChangeEvent remoteChange(RuleGroupStore.Group group, String key, RuleView rule) {
        RuleChangeEvent event = ClientRuleChanges.latest(rule.stateId());
        if (event == null || !event.newValue().equals(rule.value())) return null;
        return rule.value().equals(group.headValue(key)) ? null : event;
    }

    private Map<String, RuleRef> rulesById() {
        Map<String, RuleRef> result = new LinkedHashMap<>();
        rules().forEach(ref -> result.put(ref.key(), ref));
        return result;
    }

    private List<RuleRef> rules() {
        Map<String, RuleRef> unique = new LinkedHashMap<>();
        for (RulePage page : pages) {
            for (RuleView rule : page.rules()) {
                RuleRef ref = new RuleRef(page.title(), page.id(), page.id() + "::" + rule.stateId(), rule);
                unique.putIfAbsent(ref.key(), ref);
            }
        }
        List<RuleRef> result = new ArrayList<>(unique.values());
        result.sort(Comparator.comparing((RuleRef ref) -> ref.owner().getString(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(ref -> ref.rule().label().getString(), String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static Component tr(String key) {
        return Component.translatable("carpet-gui.workspace." + key);
    }

    private enum Mode { VIEW, MANAGE, STATUS, COMMIT }

    private record RuleRef(Component owner, String ownerId, String key, RuleView rule) {
    }

    private record MembershipRow(RuleRef ref, WorkspaceStaticPanel row) {
    }

    private record RuleChange(String key, String value, String detail, RuleRef ref, String marker, int color) {
    }

    private static final class CommitGraphNode extends TElement {
        private final boolean pending;
        private final boolean continues;

        private CommitGraphNode(boolean pending, boolean continues) {
            this.pending = pending;
            this.continues = continues;
            hoverableProperty().set(false, CommitGraphNode.class);
            focusableProperty().set(false, CommitGraphNode.class);
        }

        @Override public void renderCallback(TGuiGraphics graphics) {
            var bounds = getBounds();
            int center = bounds.x + 6;
            if (continues) graphics.fillColor(center, bounds.y + 8, 1, Math.max(0, bounds.height - 8), 0xFF6548A8);
            int color = pending ? 0xFFB68CFF : 0xFF7050C0;
            graphics.fillColor(center - 3, bounds.y + 3, 7, 7, color);
            graphics.fillColor(center - 1, bounds.y + 5, 3, 3, 0xFF202126);
        }
    }
}
//#endif

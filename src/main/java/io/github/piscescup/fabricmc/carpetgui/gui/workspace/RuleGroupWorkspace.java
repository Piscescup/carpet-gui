package io.github.piscescup.fabricmc.carpetgui.gui.workspace;

import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.panel.TPanelElement;
import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
import io.github.piscescup.fabricmc.carpetgui.gui.model.EditableRuleView;
import io.github.piscescup.fabricmc.carpetgui.gui.model.PersistentRuleEditor;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleEditResult;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RulePage;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleView;
import io.github.piscescup.fabricmc.carpetgui.network.ClientRuleChanges;
import io.github.piscescup.fabricmc.carpetgui.network.RuleChangeEvent;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Group page first; Git-like status/stage/commit/push is a separate workflow over its member rules. */
final class RuleGroupWorkspace {
    static final String ID = "carpet-gui:rule-groups";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int NEW_RULE = 0xFFFF5555;
    private static final int MODIFIED_RULE = 0xFF55AAFF;
    private static final int STAGED_RULE = 0xFF55FF55;
    private static final int REMOTE_RULE = 0xFFFFAA00;

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
    private String commitMessageDraft = "";
    private long observedRemoteRevision;

    RuleGroupWorkspace(CarpetWorkspaceScreen screen, List<? extends RulePage> pages) {
        this.screen = screen;
        this.pages = pages;
    }

    void init(int x, int y, int width, int height) {
        observedRemoteRevision = ClientRuleChanges.revision();
        activeRows.clear();
        activeRowKeys.clear();
        List<RuleGroupStore.Group> groups = RuleGroupStore.groups();
        if (selectedGroupId == null || RuleGroupStore.find(selectedGroupId) == null) {
            selectedGroupId = groups.isEmpty() ? null : groups.getFirst().id();
        }
        RuleGroupStore.Group selected = selectedGroupId == null ? null : RuleGroupStore.find(selectedGroupId);
        int actionsHeight = 34;
        int contentHeight = Math.max(1, height - actionsHeight);
        int leftWidth = Math.clamp(width * 22 / 100, Math.min(110, width / 3), 210);
        int rightWidth = Math.clamp(width * 30 / 100, Math.min(140, width / 3), 310);
        int centerWidth = Math.max(1, width - leftWidth - rightWidth);
        var left = new WorkspacePanel(8);
        var center = new WorkspacePanel(8);
        var right = new WorkspacePanel(8);
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
        WorkspaceStyle.label(panel, tr("rule_groups"), x, y, width, 16, WorkspaceStyle.ACCENT);
        y += 22;
        var create = new WorkspaceStyle.ControlButton(tr(creating ? "cancel" : "new_group"), () -> {
            creating = !creating;
            nameDraft = "";
            tagDraft = "";
            screen.rebuildWorkspace();
        });
        create.setBounds(x, y, width, WorkspaceStyle.CONTROL_HEIGHT);
        panel.add(create);
        y += 27;
        if (creating) {
            var name = new NativeTextInput(tr("group_name"), nameDraft, value -> nameDraft = value, () -> {}, this::cancelCreate);
            name.setBounds(x, y, width, WorkspaceStyle.CONTROL_HEIGHT);
            panel.add(name);
            y += 24;
            var tag = new NativeTextInput(tr("group_tag"), tagDraft, value -> tagDraft = value, () -> {}, this::cancelCreate);
            tag.setBounds(x, y, width, WorkspaceStyle.CONTROL_HEIGHT);
            panel.add(tag);
            y += 24;
            var confirm = new WorkspaceStyle.ControlButton(tr("create_group"), this::createGroup);
            confirm.setBounds(x, y, width, WorkspaceStyle.CONTROL_HEIGHT);
            panel.add(confirm);
            y += 29;
        }
        if (groups.isEmpty()) {
            paragraph(panel, tr("no_groups"), x, y + 3, width, WorkspaceStyle.MUTED);
            return;
        }
        for (RuleGroupStore.Group group : groups) {
            var item = new WorkspaceStyle.Button(Component.empty(), () -> {
                cancelDrafts();
                selectedGroupId = group.id();
                mode = Mode.VIEW;
                screen.rebuildWorkspace();
            });
            item.setSelected(group.id().equals(selectedGroupId));
            item.setBounds(x, y, width, WorkspaceStyle.RULE_HEIGHT);
            panel.add(item);
            WorkspaceStyle.label(item, Component.literal(group.name()), x + 7, y + 3,
                Math.max(1, width - 14), 10,
                group.id().equals(selectedGroupId) ? WorkspaceStyle.ACCENT : WorkspaceStyle.TEXT);
            String state = group.commitsAhead() > 0 ? "↑" + group.commitsAhead() : "✓";
            var label = WorkspaceStyle.label(item,
                Component.literal((group.tag().isBlank() ? "HEAD" : group.tag()) + "  ·  r" + group.revision() + "  " + state),
                x + 7, y + 14, Math.max(1, width - 14), 9, WorkspaceStyle.MUTED);
            label.textScaleProperty().set(WorkspaceStyle.SMALL_TEXT_SCALE, RuleGroupWorkspace.class);
            label.wrapTextProperty().set(false, RuleGroupWorkspace.class);
            y += WorkspaceStyle.RULE_HEIGHT + 3;
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
        int x = bounds.x + 9, y = bounds.y + 8, width = Math.max(320, bounds.width - 18);
        if (group == null) {
            WorkspaceStyle.label(panel, tr("group_rules"), x, y, width, 16, WorkspaceStyle.ACCENT);
            paragraph(panel, tr("select_or_create_group"), x, y + 24, width, WorkspaceStyle.MUTED);
            return;
        }
        WorkspaceStyle.label(panel, Component.literal(group.name()), x, y, Math.max(1, width - 100), 16, WorkspaceStyle.ACCENT);
        var manage = new WorkspaceStyle.ControlButton(tr("manage_rules"), () -> setMode(Mode.MANAGE));
        manage.setBounds(x + width - 94, y - 3, 94, WorkspaceStyle.CONTROL_HEIGHT);
        panel.add(manage);
        y += 24;
        addStatusLegend(panel, x, y, width);
        y += 17;
        Map<String, RuleRef> byId = rulesById();
        if (group.members().isEmpty()) {
            y = paragraph(panel, tr("empty_group"), x, y + 8, width, WorkspaceStyle.MUTED) + 12;
            var addRules = new WorkspaceStyle.ControlButton(tr("add_group_rules"), () -> setMode(Mode.MANAGE));
            addRules.setBounds(x, y, Math.min(150, width), WorkspaceStyle.CONTROL_HEIGHT);
            panel.add(addRules);
            return;
        }
        for (String ruleId : group.members()) {
            RuleRef ref = byId.get(ruleId);
            if (ref == null) {
                addMembershipRow(panel, null, ruleId, true, group, x, y, width);
                y += WorkspaceStyle.RULE_HEIGHT + 3;
                continue;
            }
            var row = new WorkspaceRuleRow(ref.ownerId(), ref.rule(), result -> screen.feedback(ref.rule(), result),
                this::cancelDrafts, () -> ruleColor(group, ruleId, ref.rule()),
                () -> ruleDetail(group, ruleId, ref.rule()));
            row.setBounds(x, y, width, WorkspaceStyle.RULE_HEIGHT);
            panel.add(row);
            activeRows.add(row);
            activeRowKeys.put(row, ruleId);
            y += WorkspaceStyle.RULE_HEIGHT + 3;
        }
    }

    /** Membership picker. Adding a member does not stage or commit it. */
    private void initManageRules(WorkspacePanel panel, RuleGroupStore.Group group) {
        var bounds = panel.getBounds();
        int x = bounds.x + 9, y = bounds.y + 8, width = Math.max(320, bounds.width - 18);
        WorkspaceStyle.label(panel, tr("manage_group_rules"), x, y, Math.max(1, width - 70), 16, WorkspaceStyle.ACCENT);
        var done = new WorkspaceStyle.ControlButton(tr("done"), () -> setMode(Mode.VIEW));
        done.setBounds(x + width - 64, y - 3, 64, WorkspaceStyle.CONTROL_HEIGHT);
        panel.add(done);
        y += 25;
        if (group == null) {
            paragraph(panel, tr("select_or_create_group"), x, y, width, WorkspaceStyle.MUTED);
            return;
        }
        List<RuleRef> rules = rules();
        for (RuleRef ref : rules) {
            addMembershipRow(panel, ref, ref.key(), group.members().contains(ref.key()), group, x, y, width);
            y += 29;
        }
        if (rules.isEmpty()) paragraph(panel, tr("no_rules"), x, y, width, WorkspaceStyle.MUTED);
    }

    private void addMembershipRow(TElement parent, RuleRef ref, String key, boolean member, RuleGroupStore.Group group,
                                  int x, int y, int width) {
        var row = new WorkspaceStaticPanel(WorkspaceStyle.RULE_BACKGROUND, WorkspaceStyle.BORDER, WorkspaceStyle.BORDER);
        row.setBounds(x, y, width, 26);
        parent.add(row);
        int actionWidth = Math.clamp(width / 5, 46, 70);
        WorkspaceStyle.label(row, Component.literal(member ? "●" : "+"), x + 6, y + 7, 12, 11,
            member ? WorkspaceStyle.ACCENT : WorkspaceStyle.MUTED);
        Component title = ref == null ? Component.literal(key) : ref.rule().label();
        String detail = ref == null ? tr("missing_rule").getString()
            : ref.owner().getString() + "  ·  " + ref.rule().value();
        int titleColor = member && ref != null ? ruleColor(group, key, ref.rule()) : WorkspaceStyle.TEXT;
        WorkspaceStyle.label(row, title, x + 21, y + 3, Math.max(1, width - actionWidth - 29), 10, titleColor);
        var detailLabel = WorkspaceStyle.label(row, Component.literal(detail), x + 21, y + 14,
            Math.max(1, width - actionWidth - 29), 9, WorkspaceStyle.MUTED);
        detailLabel.textScaleProperty().set(WorkspaceStyle.SMALL_TEXT_SCALE, RuleGroupWorkspace.class);
        detailLabel.wrapTextProperty().set(false, RuleGroupWorkspace.class);
        Runnable action = member
            ? () -> mutate(() -> RuleGroupStore.removeMember(group, key))
            : () -> mutate(() -> RuleGroupStore.addMember(group, key));
        var button = new WorkspaceStyle.Button(tr(member ? "remove" : "add_rule"), action);
        button.setBounds(x + width - actionWidth - 3, y + 3, actionWidth, 20);
        row.add(button);
    }

    /** Status GUI: staged and unstaged are deliberately distinct. */
    private void initStatus(WorkspacePanel panel, RuleGroupStore.Group group) {
        var bounds = panel.getBounds();
        int x = bounds.x + 9, y = bounds.y + 8, width = Math.max(320, bounds.width - 18);
        WorkspaceStyle.label(panel, tr("status_title"), x, y, Math.max(1, width - 70), 16, WorkspaceStyle.ACCENT);
        var back = new WorkspaceStyle.ControlButton(tr("back"), () -> setMode(Mode.VIEW));
        back.setBounds(x + width - 64, y - 3, 64, WorkspaceStyle.CONTROL_HEIGHT);
        panel.add(back);
        y += 25;
        if (group == null) {
            paragraph(panel, tr("select_or_create_group"), x, y, width, WorkspaceStyle.MUTED);
            return;
        }
        Map<String, RuleRef> byId = rulesById();
        List<RuleChange> unstaged = changes(group, byId);
        WorkspaceStyle.label(panel, Component.translatable("carpet-gui.workspace.unstaged_changes", unstaged.size()),
            x, y, width, 15, WorkspaceStyle.ACCENT);
        y += 20;
        if (unstaged.isEmpty()) y = paragraph(panel, tr("no_unstaged_changes"), x + 8, y, width - 8, WorkspaceStyle.MUTED) + 7;
        for (RuleChange change : unstaged) {
            addChangeRow(panel, change.ref(), change.key(), change.detail(), change.marker(), change.color(), "stage",
                () -> mutate(() -> RuleGroupStore.stage(group, change.key(), change.value())), x, y, width);
            y += 29;
        }
        y += 5;
        WorkspaceStyle.label(panel, Component.translatable("carpet-gui.workspace.staged_changes", group.staged().size()),
            x, y, width, 15, WorkspaceStyle.ACCENT);
        y += 20;
        if (group.staged().isEmpty()) y = paragraph(panel, tr("no_staged_changes"), x + 8, y, width - 8, WorkspaceStyle.MUTED) + 7;
        for (Map.Entry<String, String> staged : group.staged().entrySet()) {
            addChangeRow(panel, byId.get(staged.getKey()), staged.getKey(), staged.getValue(), "A", STAGED_RULE, "unstage",
                () -> mutate(() -> RuleGroupStore.unstage(group, staged.getKey())), x, y, width);
            y += 29;
        }
        y += 8;
        var restore = new WorkspaceStyle.ControlButton(tr("restore_head"), () -> restore(group));
        restore.setBounds(x, y, Math.min(145, width), WorkspaceStyle.CONTROL_HEIGHT);
        panel.add(restore);
    }

    private void initCommit(WorkspacePanel panel, RuleGroupStore.Group group) {
        var bounds = panel.getBounds();
        int x = bounds.x + 9, y = bounds.y + 8, width = Math.max(320, bounds.width - 18);
        WorkspaceStyle.label(panel, tr("create_commit"), x, y, Math.max(1, width - 70), 16, WorkspaceStyle.ACCENT);
        var back = new WorkspaceStyle.ControlButton(tr("back"), () -> setMode(Mode.STATUS));
        back.setBounds(x + width - 64, y - 3, 64, WorkspaceStyle.CONTROL_HEIGHT);
        panel.add(back);
        y += 28;
        if (group == null) {
            paragraph(panel, tr("select_or_create_group"), x, y, width, WorkspaceStyle.MUTED);
            return;
        }
        WorkspaceStyle.label(panel, tr("commit_message"), x, y, width, 13, WorkspaceStyle.TEXT);
        y += 17;
        var message = new NativeTextInput(tr("commit_message_hint"), commitMessageDraft,
            value -> commitMessageDraft = value, () -> {}, () -> setMode(Mode.STATUS));
        message.setBounds(x, y, width, WorkspaceStyle.CONTROL_HEIGHT);
        panel.add(message);
        y += 29;
        var commit = new WorkspaceStyle.ControlButton(Component.translatable("carpet-gui.workspace.commit_count", group.staged().size()),
            () -> commitNow(group));
        commit.setBounds(x, y, Math.min(170, width), WorkspaceStyle.CONTROL_HEIGHT);
        commit.enabledProperty().set(!group.staged().isEmpty(), RuleGroupWorkspace.class);
        panel.add(commit);
        y += 31;
        WorkspaceStyle.label(panel, Component.translatable("carpet-gui.workspace.staged_changes", group.staged().size()),
            x, y, width, 15, WorkspaceStyle.ACCENT);
        y += 20;
        Map<String, RuleRef> byId = rulesById();
        for (Map.Entry<String, String> staged : group.staged().entrySet()) {
            addChangeRow(panel, byId.get(staged.getKey()), staged.getKey(), staged.getValue(), "A", STAGED_RULE,
                null, null, x, y, width);
            y += 29;
        }
        if (group.staged().isEmpty()) paragraph(panel, tr("nothing_to_commit"), x + 8, y, width - 8, WorkspaceStyle.MUTED);
    }

    private void addChangeRow(TElement parent, RuleRef ref, String key, String value, String marker, int color,
                              String actionKey, Runnable action, int x, int y, int width) {
        var row = new WorkspaceStaticPanel(WorkspaceStyle.RULE_BACKGROUND, WorkspaceStyle.BORDER, WorkspaceStyle.BORDER);
        row.setBounds(x, y, width, 26);
        parent.add(row);
        int actionWidth = action == null ? 0 : Math.clamp(width / 5, 46, 70);
        WorkspaceStyle.label(row, Component.literal(marker), x + 6, y + 7, 12, 11, color);
        Component title = ref == null ? Component.literal(key) : ref.rule().label();
        String owner = ref == null ? tr("missing_rule").getString() : ref.owner().getString();
        WorkspaceStyle.label(row, title, x + 21, y + 3, Math.max(1, width - actionWidth - 29), 10, color);
        var detail = WorkspaceStyle.label(row, Component.literal(owner + "  ·  " + value), x + 21, y + 14,
            Math.max(1, width - actionWidth - 29), 9, WorkspaceStyle.MUTED);
        detail.textScaleProperty().set(WorkspaceStyle.SMALL_TEXT_SCALE, RuleGroupWorkspace.class);
        detail.wrapTextProperty().set(false, RuleGroupWorkspace.class);
        if (action != null) {
            var button = new WorkspaceStyle.Button(tr(actionKey), action);
            button.setBounds(x + width - actionWidth - 3, y + 3, actionWidth, 20);
            row.add(button);
        }
    }

    private void initActions(int x, int y, int width, int height, RuleGroupStore.Group group) {
        var bar = new WorkspaceStaticPanel(0x55000000, WorkspaceStyle.BORDER, WorkspaceStyle.BORDER);
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
        var button = new WorkspaceStyle.Button(tr(key), action);
        button.setSelected(selected);
        button.enabledProperty().set(enabled, RuleGroupWorkspace.class);
        button.setBounds(x, y, width, WorkspaceStyle.CONTROL_HEIGHT);
        parent.add(button);
    }

    /** Right-hand Git log: graph line, commit message, refs, time and pushed state. */
    private void initHistory(WorkspacePanel panel, RuleGroupStore.Group group) {
        var bounds = panel.getBounds();
        int x = bounds.x + 9, y = bounds.y + 8, width = Math.max(1, bounds.width - 18);
        WorkspaceStyle.label(panel, tr("process"), x, y, width, 16, WorkspaceStyle.ACCENT);
        y += 23;
        if (group == null) {
            paragraph(panel, tr("no_group_selected"), x, y, width, WorkspaceStyle.MUTED);
            return;
        }
        WorkspaceStyle.label(panel, Component.literal(group.name()), x, y, width, 16, WorkspaceStyle.TEXT);
        y += 19;
        Component summary = Component.translatable("carpet-gui.workspace.pipeline_summary", group.members().size(),
            group.staged().size(), group.commitsAhead());
        y = paragraph(panel, summary, x, y, width, WorkspaceStyle.MUTED) + 14;
        if (group.history().isEmpty()) {
            paragraph(panel, tr("no_commits_graph"), x, y, width, WorkspaceStyle.MUTED);
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
                Math.max(1, width - 37), 12, WorkspaceStyle.TEXT).wrapTextProperty().set(false, RuleGroupWorkspace.class);
            String refs = "r" + commit.revision();
            if (index == 0 && !group.tag().isBlank()) refs += "  ◇ " + group.tag();
            refs += pending ? "  local" : "  server";
            var metadata = WorkspaceStyle.label(panel, Component.literal(refs), x + 18, y + 14,
                Math.max(1, width - 37), 9, pending ? WorkspaceStyle.FOCUS : WorkspaceStyle.MUTED);
            metadata.textScaleProperty().set(WorkspaceStyle.SMALL_TEXT_SCALE, RuleGroupWorkspace.class);
            String time = TIME.format(Instant.ofEpochMilli(commit.time()).atZone(ZoneId.systemDefault()));
            var date = WorkspaceStyle.label(panel, Component.literal(time), x + 18, y + 24,
                Math.max(1, width - 37), 9, WorkspaceStyle.MUTED);
            date.textScaleProperty().set(WorkspaceStyle.SMALL_TEXT_SCALE, RuleGroupWorkspace.class);
            WorkspaceStyle.label(panel, Component.literal(pending ? "↑" : "✓"), x + width - 16, y + 3, 14, 12,
                pending ? WorkspaceStyle.ACCENT : 0xFF55FF55);
            y += 39;
        }
    }

    private List<RuleChange> changes(RuleGroupStore.Group group, Map<String, RuleRef> byId) {
        List<RuleChange> result = new ArrayList<>();
        for (String key : group.members()) {
            RuleRef ref = byId.get(key);
            if (ref == null) continue;
            String current = draftSnapshot.getOrDefault(key, ref.rule().value());
            String baseline = group.isStaged(key) ? group.stagedValue(key) : group.headValue(key);
            if (baseline == null || !baseline.equals(current)) {
                boolean isNew = !group.tracked(key) && !group.isStaged(key);
                RuleChangeEvent remote = remoteChange(group, key, ref.rule());
                String detail = remote == null ? current
                    : remote.oldValue() + " → " + remote.newValue() + "  ·  " + remote.actorName();
                result.add(new RuleChange(key, current, detail, ref,
                    remote != null ? "R" : isNew ? "?" : "M",
                    remote != null ? REMOTE_RULE : isNew ? NEW_RULE : MODIFIED_RULE));
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

    private void addStatusLegend(TElement parent, int x, int y, int width) {
        int column = Math.max(1, width / 5);
        addLegendItem(parent, tr("color_new"), x, y, column, NEW_RULE);
        addLegendItem(parent, tr("color_modified"), x + column, y, column, MODIFIED_RULE);
        addLegendItem(parent, tr("color_staged"), x + column * 2, y, column, STAGED_RULE);
        addLegendItem(parent, tr("color_remote"), x + column * 3, y, column, REMOTE_RULE);
        addLegendItem(parent, tr("color_committed"), x + column * 4, y, width - column * 4, WorkspaceStyle.TEXT);
    }

    private void addLegendItem(TElement parent, Component text, int x, int y, int width, int color) {
        var label = WorkspaceStyle.label(parent, Component.literal("● ").append(text), x, y, width, 11, color);
        label.textScaleProperty().set(WorkspaceStyle.SMALL_TEXT_SCALE, RuleGroupWorkspace.class);
        label.wrapTextProperty().set(false, RuleGroupWorkspace.class);
    }

    private int ruleColor(RuleGroupStore.Group group, String key, RuleView rule) {
        String current = rule.value();
        if (group.isStaged(key)) {
            if (current.equals(group.stagedValue(key))) return STAGED_RULE;
        }
        if (remoteChange(group, key, rule) != null) return REMOTE_RULE;
        if (group.isStaged(key)) return MODIFIED_RULE;
        if (!group.tracked(key)) return NEW_RULE;
        return current.equals(group.headValue(key)) ? WorkspaceStyle.TEXT : MODIFIED_RULE;
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

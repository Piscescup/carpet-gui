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

import com.thecsdev.common.util.enumerations.CompassDirection;
import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.ctxmenu.TContextMenu;
import com.thecsdev.commonmc.api.client.gui.label.TLabelElement;
import com.thecsdev.commonmc.api.client.gui.misc.TTextureElement;
import com.thecsdev.commonmc.api.client.gui.panel.TPanelElement;
import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
import com.thecsdev.commonmc.api.client.gui.tooltip.TTooltip;
import com.thecsdev.commonmc.api.client.gui.util.TInputContext;
import com.thecsdev.commonmc.api.client.gui.widget.TClickableWidget;
import com.thecsdev.commonmc.api.client.gui.widget.TCheckboxWidget;
import io.github.piscescup.fabricmc.carpetgui.gui.model.*;
import io.github.piscescup.fabricmc.carpetgui.gui.tabstrip.WorkspaceTabStripPanel;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.button.Button;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.NativeTextInput;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.button.ControlButton;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.button.IconButton;
import io.github.piscescup.fabricmc.carpetgui.network.ClientRuleConfigurations;
import io.github.piscescup.fabricmc.carpetgui.store.ModIconStore;
import io.github.piscescup.fabricmc.carpetgui.util.Msg;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

//#if MC >= 260300
import static org.lwjgl.sdl.SDLMouse.SDL_BUTTON_LEFT;
//#else
//$$ import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
//#endif

/** Owns the tab strip, selected page, refresh state and document interactions. */
public final class WorkspaceDocumentInterface extends TElement {
    private final CarpetWorkspaceScreen screen;
    private final CarpetWorkspaceEditor editor;
    private final RuleGroupWorkspace ruleGroups;
    private final ModIconStore icons = ModIconStore.INSTANCE;
    private WorkspacePageElement contentRoot;
    private RuleListPanel ruleList;
    private TLabelElement notice;
    private TLabelElement counts;
    private TLabelElement homeCounts;
    private TLabelElement connectionStatus;
    private TLabelElement homeHelpHeading;
    private TLabelElement homeHelpBody;
    private TCheckboxWidget modifiedFilter;
    private TCheckboxWidget savedDefaultFilter;
    private Component feedback = Component.empty();
    private boolean listDirty;
    private boolean resetScroll;
    private int refreshTicks;
    private long observedRevision;
    private long observedNavigationRevision;
    private List<String> observedCategories = List.of();

    public WorkspaceDocumentInterface(CarpetWorkspaceScreen screen, CarpetWorkspaceEditor editor) {
        this.screen = Objects.requireNonNull(screen, "screen");
        this.editor = Objects.requireNonNull(editor, "editor");
        this.ruleGroups = new RuleGroupWorkspace(screen, editor.sourcePages());
    }

    void cancelDrafts() {
        if (ruleList != null) ruleList.cancelDrafts();
        ruleGroups.cancelDrafts();
    }

    void closeResources() {
        cancelDrafts();
        icons.close();
    }
    private RulePage page() {
        return editor.selectedPage();
    }

    @Override
    protected void initCallback() {
        if (ruleList != null) ruleList.cancelDrafts();
        ruleGroups.cancelDrafts();
        ruleList = null;
        notice = null;
        counts = null;
        homeCounts = null;
        connectionStatus = null;
        homeHelpHeading = null;
        homeHelpBody = null;
        modifiedFilter = null;
        savedDefaultFilter = null;
        var screen = getBounds();
        int x = Math.max(4, screen.width / 40);
        int width = Math.max(1, screen.width - 2 * x);
        int bodyY = WorkspaceStyle.BODY_Y;
        int bodyHeight = Math.max(1, screen.height - bodyY - 10);
        var frame = new TPanelElement.Paintable(WorkspaceStyle.FRAME_COLOR, WorkspaceStyle.BORDER_COLOR, WorkspaceStyle.BORDER_COLOR);
        frame.setBounds(x, WorkspaceStyle.TAB_Y - 1, width, bodyY + bodyHeight - WorkspaceStyle.TAB_Y + 1);
        frame.hoverableProperty().set(false, WorkspaceDocumentInterface.class);
        frame.focusableProperty().set(false, WorkspaceDocumentInterface.class);
        add(frame);
        buildTabStrip(x, width);
        contentRoot = WorkspacePageElement.create(this, editor);
        contentRoot.setBounds(x, bodyY, width, bodyHeight);
        add(contentRoot);
        notice = WorkspaceStyle.label(this, Component.empty(), x + 4, screen.height - 18, width - 8, 14, WorkspaceStyle.MUTED_COLOR);
        notice.wrapTextProperty()
            .set(false, WorkspaceDocumentInterface.class);
        observedRevision = editor.revision();
        observedNavigationRevision = editor.navigationRevision();
        listDirty = false;
    }

    private void buildTabStrip(int x, int width) {
        var tabs = new WorkspaceTabStripPanel(editor);
        tabs.setBounds(x + 1, WorkspaceStyle.TAB_Y, Math.max(1, width - 2), WorkspaceStyle.TAB_HEIGHT);
        add(tabs);
    }

    void buildRulesPage(RulePage page, int x, int y, int width, int height) {
        RuleBrowserModel model = RuleBrowserModel.forPage(page.id());
        observedCategories = model.categories(page);
        if (!model.category.isEmpty() && !model.category.equals(RuleBrowserModel.FAVORITES)
            && !model.categories(page).contains(model.category)) {
            model.category = "";
        }
        int sidebarWidth = Math.clamp(width * 30 / 100, Math.min(120, width / 3), 235);
        var sidebar = new WorkspacePanel(12);
        addWorkspacePane(sidebar, x, y, sidebarWidth, height);
        buildRulesSidebar(sidebar, page, model);
        int listX = x + sidebarWidth;
        int listWidth = Math.max(1, width - sidebarWidth);
        counts = WorkspaceStyle.label(contentRoot, Component.empty(), listX + 10, y + 8, listWidth - 20, 18, WorkspaceStyle.MUTED_COLOR);
        ruleList = new RuleListPanel(screen, page, model);
        addWorkspacePane(ruleList, listX, y + 23, listWidth, Math.max(1, height - 23));
    }

    private void buildRulesSidebar(TPanelElement sidebar, RulePage page, RuleBrowserModel model) {
        var bounds = sidebar.getBounds();
        int x = bounds.x + 9, width = Math.max(1, bounds.width - 18), y = bounds.y + 9;
        var filterTitle = WorkspaceStyle.label(sidebar, Msg.tr("filters"), x, y, width, 18, WorkspaceStyle.TEXT_COLOR);
        filterTitle.textAlignmentProperty().set(CompassDirection.CENTER, WorkspaceDocumentInterface.class);
        y += 23;
        List<WorkspaceStyle.Option<String>> categories = new ArrayList<>();
        categories.add(new WorkspaceStyle.Option<>("", Msg.tr("all_categories")));
        model.categories(page)
            .forEach(category -> categories.add(new WorkspaceStyle.Option<>(category, categoryLabel(page, category))));
        categories.add(new WorkspaceStyle.Option<>(RuleBrowserModel.FAVORITES, Msg.tr("favorites")));
        addDropdown(
            sidebar, x, y, width, categories, model.category, category -> {
                model.category = category;
                requestListRefresh(true);
            }
        );
        y += 25;
        var search = new NativeTextInput(
            Msg.tr("search"), model.query, query -> {
            model.query = query;
            requestListRefresh(true);
        },
            () -> {
            }, () -> screen.focusedElementProperty().set(null, WorkspaceDocumentInterface.class)
        );
        search.setBounds(x, y, width, WorkspaceStyle.CONTROL_HEIGHT);
        search.tooltipProperty()
            .set(ignored -> TTooltip.of(Msg.tr("search_hint")), WorkspaceDocumentInterface.class);
        sidebar.add(search);
        y += 25;
        if (!ClientRuleConfigurations.ready()) model.modifiedOnly = false;
        modifiedFilter = new TCheckboxWidget(model.modifiedOnly);
        modifiedFilter.setBounds(x, y, WorkspaceStyle.CONTROL_HEIGHT, WorkspaceStyle.CONTROL_HEIGHT);
        modifiedFilter.enabledProperty().set(ClientRuleConfigurations.ready() && !page.isVanilla(), WorkspaceDocumentInterface.class);
        modifiedFilter.checkedProperty().addChangeListener((property, previous, checked) -> {
            model.modifiedOnly = checked;
            requestListRefresh(true);
        });
        Component modifiedHint = Msg.tr("modified_only_hint").copy()
            .append("\n")
            .append(Msg.tr("modified_only_server_required").copy().withStyle(ChatFormatting.GOLD));
        modifiedFilter.tooltipProperty().set(ignored -> TTooltip.of(modifiedHint), WorkspaceDocumentInterface.class);
        sidebar.add(modifiedFilter);
        var modifiedLabel = WorkspaceStyle.label(sidebar, Msg.tr("modified_only"), x + 27, y + 6, width - 27, 12, WorkspaceStyle.TEXT_COLOR);
        modifiedLabel.tooltipProperty().set(ignored -> TTooltip.of(modifiedHint), WorkspaceDocumentInterface.class);
        y += 27;
        var initialDifference = new TCheckboxWidget(model.initialDifferenceOnly);
        initialDifference.setBounds(x, y, WorkspaceStyle.CONTROL_HEIGHT, WorkspaceStyle.CONTROL_HEIGHT);
        initialDifference.checkedProperty().addChangeListener((property, previous, checked) -> {
            model.initialDifferenceOnly = checked;
            requestListRefresh(true);
        });
        initialDifference.tooltipProperty().set(
            ignored -> TTooltip.of(Msg.tr("initial_difference_hint")), WorkspaceDocumentInterface.class
        );
        sidebar.add(initialDifference);
        var initialDifferenceLabel = WorkspaceStyle.label(
            sidebar, Msg.tr("initial_difference_only"), x + 27, y + 6, width - 27, 12, WorkspaceStyle.TEXT_COLOR
        );
        initialDifferenceLabel.tooltipProperty().set(
            ignored -> TTooltip.of(Msg.tr("initial_difference_hint")), WorkspaceDocumentInterface.class
        );
        y += 27;
        if (!ClientRuleConfigurations.ready()) model.savedDefaultOnly = false;
        savedDefaultFilter = new TCheckboxWidget(model.savedDefaultOnly);
        savedDefaultFilter.setBounds(x, y, WorkspaceStyle.CONTROL_HEIGHT, WorkspaceStyle.CONTROL_HEIGHT);
        savedDefaultFilter.enabledProperty().set(ClientRuleConfigurations.ready() && !page.isVanilla(), WorkspaceDocumentInterface.class);
        savedDefaultFilter.checkedProperty().addChangeListener((property, previous, checked) -> {
            model.savedDefaultOnly = checked;
            requestListRefresh(true);
        });
        Component savedDefaultHint = Msg.tr("saved_default_only_hint").copy()
            .append("\n")
            .append(Msg.tr("modified_only_server_required").copy().withStyle(ChatFormatting.GOLD));
        savedDefaultFilter.tooltipProperty().set(ignored -> TTooltip.of(savedDefaultHint), WorkspaceDocumentInterface.class);
        sidebar.add(savedDefaultFilter);
        var savedDefaultLabel = WorkspaceStyle.label(
            sidebar, Msg.tr("saved_default_only"), x + 27, y + 6, width - 27, 12, WorkspaceStyle.TEXT_COLOR
        );
        savedDefaultLabel.tooltipProperty().set(ignored -> TTooltip.of(savedDefaultHint), WorkspaceDocumentInterface.class);
        y += 27;
        addIconDropdown(
            sidebar,
            x,
            y,
            width,
            WorkspaceIcon.Kind.SORT,
            Msg.tr("sort"),
            options(
                RuleBrowserModel.Sort.values(),
                value -> Msg.tr("sort." + value.name()
                    .toLowerCase(Locale.ROOT))
            ),
            model.sort,
            sort -> {
                model.sort = sort;
                requestListRefresh(true);
            }
        );
        y += 27;
        addIconDropdown(sidebar, x, y, width, WorkspaceIcon.Kind.GROUP, Msg.tr("grouping"),
            options(RuleBrowserModel.Grouping.values(), value -> Msg.tr("grouping." + value.name().toLowerCase(Locale.ROOT))),
            model.grouping, grouping -> {
                model.grouping = grouping;
                requestListRefresh(true);
            });
        y += 27;
        addIconDropdown(
            sidebar,
            x,
            y,
            width,
            WorkspaceIcon.Kind.DISTANCE,
            Msg.tr("distance"),
            options(
                RuleBrowserModel.Distance.values(),
                value -> Msg.tr("distance." + value.name()
                    .toLowerCase(Locale.ROOT))
            ),
            model.distance,
            value -> model.distance = value
        );
        y += 27;
        addIconDropdown(
            sidebar,
            x,
            y,
            width,
            WorkspaceIcon.Kind.TIME,
            Msg.tr("time"),
            options(
                RuleBrowserModel.Time.values(),
                value -> Msg.tr("time." + value.name()
                    .toLowerCase(Locale.ROOT))
            ),
            model.time,
            value -> model.time = value
        );
        y += 29;
        int gap = 4;
        int expandWidth = Math.max(1, (width - gap) / 2);
        int collapseWidth = Math.max(1, width - gap - expandWidth);
        var expand = new ControlButton(Component.translatable("carpet-gui.expand_all"), () -> expandAll(true));
        expand.setBounds(x, y, expandWidth, WorkspaceStyle.CONTROL_HEIGHT);
        expand.getLabel().textScaleProperty().set(Math.min(WorkspaceStyle.TEXT_SCALE,
            Math.max(1, expandWidth - 8) / (double) Math.max(1, getClient().font.width(expand.getLabel().getText()))), WorkspaceDocumentInterface.class);
        sidebar.add(expand);
        var collapse = new ControlButton(Component.translatable("carpet-gui.collapse_all"), () -> expandAll(false));
        collapse.setBounds(x + expandWidth + gap, y, collapseWidth, WorkspaceStyle.CONTROL_HEIGHT);
        collapse.getLabel().textScaleProperty().set(Math.min(WorkspaceStyle.TEXT_SCALE,
            Math.max(1, collapseWidth - 8) / (double) Math.max(1, getClient().font.width(collapse.getLabel().getText()))), WorkspaceDocumentInterface.class);
        sidebar.add(collapse);
    }

    void buildHomePage(int x, int y, int width, int height) {
        x += 12;
        y += 12;
        width = Math.max(1, width - 24);
        height = Math.max(1, height - 24);
        int leftWidth = Math.max(1, width * 2 / 3 - 5);
        var left = new WorkspacePanel(8);
        var right = new WorkspacePanel(12);
        addWorkspacePane(left, x, y, leftWidth, height);
        addWorkspacePane(right, x + leftWidth + 8, y, Math.max(1, width - leftWidth - 8), height);
        int contentX = x + 12, contentWidth = Math.max(1, left.getBounds().width - 24), nextY = y + 12;
        addHomeCard(left, contentX - 4, nextY - 4, contentWidth + 8, 40);
        WorkspaceStyle.label(left, Component.literal("Carpet GUI"), contentX + 38, nextY + 1, contentWidth - 38, 13, WorkspaceStyle.ACCENT_COLOR);
        WorkspaceStyle.label(left, Component.literal(version()), contentX + 38, nextY + 18, contentWidth - 38, 13, WorkspaceStyle.MUTED_COLOR);
        addIcon(left, "carpet-gui", contentX, nextY, 30);
        nextY += 51;
        WorkspaceStyle.label(left, Msg.tr("quick_access"), contentX, nextY, contentWidth, 16, WorkspaceStyle.ACCENT_COLOR);
        nextY += 25;
        int columns = Math.max(1, contentWidth / 42);
        int quickAccessHeight = Math.max(1, (editor.sourcePages().size() + columns - 1) / columns) * 42;
        addHomeCard(left, contentX - 4, nextY - 4, contentWidth + 8, quickAccessHeight + 2);
        for (int index = 0; index < editor.sourcePages().size(); index++) {
            RulePage page = editor.sourcePages().get(index);
            int tileX = contentX + (index % columns) * 42, tileY = nextY + (index / columns) * 42;
            var tile = new IconButton(() -> selectPage(page.id())) {
                @Override protected void initCallback() {
                    super.initCallback();
                    var bounds = getBounds();
                    addIcon(this, page.id(), bounds.x + 5, bounds.y + 5, 26);
                }
            };
            tile.setBounds(tileX, tileY, 36, 36);
            tile.tooltipProperty()
                .set(ignored -> TTooltip.of(page.title()), WorkspaceDocumentInterface.class);
            left.add(tile);
        }
        nextY += quickAccessHeight + 16;
        WorkspaceStyle.label(left, Msg.tr("features"), contentX, nextY, contentWidth, 16, WorkspaceStyle.ACCENT_COLOR);
        nextY += 22;
        addHomeCard(left, contentX - 4, nextY - 4, contentWidth + 8, 46);
        var groupsFeature = new Button(Msg.tr("rule_groups"), () -> selectPage(RuleGroupWorkspace.ID)) {
            @Override protected void initCallback() {
                super.initCallback();
                var bounds = getBounds();
                getLabel().setBounds(bounds.x + 39, bounds.y + 4, Math.max(1, bounds.width - 45), 14);
                getLabel().textAlignmentProperty().set(CompassDirection.WEST, WorkspaceDocumentInterface.class);
                var description = WorkspaceStyle.label(this, Msg.tr("rule_groups_home"), bounds.x + 39, bounds.y + 21,
                    Math.max(1, bounds.width - 45), 11, WorkspaceStyle.MUTED_COLOR
                );
                description.textScaleProperty().set(WorkspaceStyle.SMALL_TEXT_SCALE, WorkspaceDocumentInterface.class);
                description.wrapTextProperty().set(false, WorkspaceDocumentInterface.class);
                addIcon(this, "carpet-gui", bounds.x + 6, bounds.y + 7, 27);
            }
        };
        groupsFeature.setBounds(contentX, nextY, contentWidth, 38);
        groupsFeature.tooltipProperty().set(ignored -> TTooltip.of(Msg.tr("rule_groups_home")), WorkspaceDocumentInterface.class);
        left.add(groupsFeature);
        nextY += 56;
        WorkspaceStyle.label(left, Msg.tr("news"), contentX, nextY, contentWidth, 18, WorkspaceStyle.ACCENT_COLOR);
        // nextY = addParagraph(left, tr("news_intro"), contentX, nextY + 22, contentWidth, WorkspaceStyle.MUTED) + 16;
        // nextY = addParagraph(left, tr("news_workspace"), contentX, nextY, contentWidth, WorkspaceStyle.TEXT) + 16;
        // addParagraph(left, tr("news_addons"), contentX, nextY, contentWidth, WorkspaceStyle.TEXT);
        var rightBounds = right.getBounds();
        int rightX = rightBounds.x + 12, rightWidth = Math.max(1, rightBounds.width - 24), rightY = y + 12;
        WorkspaceStyle.label(right, Msg.tr("overview"), rightX, rightY, rightWidth, 18, WorkspaceStyle.ACCENT_COLOR);
        homeCounts = WorkspaceStyle.label(
            right, Component.translatable(
                "carpet-gui.workspace.overview_counts",
                editor.sourcePages().stream()
                    .filter(page -> !page.isVanilla())
                    .count(),
                editor.sourcePages().stream()
                    .mapToInt(page -> page.rules()
                        .size())
                    .sum()
            ), rightX, rightY + 26, rightWidth, 40, WorkspaceStyle.TEXT_COLOR
        );
        connectionStatus = WorkspaceStyle.label(
            right,
            Msg.tr(getClient().getConnection() == null ? "offline" : "connected"),
            rightX,
            rightY + 72,
            rightWidth,
            33,
            WorkspaceStyle.MUTED_COLOR
        );
        homeHelpHeading = WorkspaceStyle.label(right, Msg.tr("help"), rightX, rightY + 125, rightWidth, 18, WorkspaceStyle.ACCENT_COLOR);
        homeHelpBody = WorkspaceStyle.label(right, Msg.tr("help_body"), rightX, rightY + 152, rightWidth, 10, WorkspaceStyle.TEXT_COLOR);
        // Fit height within the available column, rather than expanding labels to their full text width.
        homeCounts.wrapTextProperty().set(true, WorkspaceDocumentInterface.class);
        connectionStatus.wrapTextProperty().set(true, WorkspaceDocumentInterface.class);
        homeHelpBody.wrapTextProperty().set(true, WorkspaceDocumentInterface.class);
        layoutHomeOverview();
    }

    private void addHomeCard(TElement parent, int x, int y, int width, int height) {
        var card = new TPanelElement.Paintable(WorkspaceStyle.CARD_COLOR, 0, 0);
        card.setBounds(x, y, width, height);
        card.hoverableProperty().set(false, WorkspaceDocumentInterface.class);
        card.focusableProperty().set(false, WorkspaceDocumentInterface.class);
        parent.add(card);
    }

    private void layoutHomeOverview() {
        if (homeCounts == null || connectionStatus == null || homeHelpBody == null) return;
        var bounds = homeCounts.getBounds();
        int x = bounds.x, y = bounds.y, width = bounds.width;
        homeCounts.setBoundsToFitText(x, y, width);
        connectionStatus.setBoundsToFitText(x, homeCounts.getBounds().endY + 12, width);
        homeHelpHeading.setBounds(x, connectionStatus.getBounds().endY + 24, width, 18);
        homeHelpBody.setBoundsToFitText(x, homeHelpHeading.getBounds().endY + 9, width);
    }

    private int addParagraph(TElement parent, Component text, int x, int y, int width, int color) {
        var label = WorkspaceStyle.label(parent, text, x, y, width, 10, color);
        label.wrapTextProperty().set(true, WorkspaceDocumentInterface.class);
        label.setBoundsToFitText(x, y, Math.max(1, width));
        return label.getBounds().endY;
    }

    void addWorkspacePane(TPanelElement panel, int x, int y, int width, int height) {
        // Child initialization is recursive and happens after this screen's init callback returns.
        panel.setBounds(x, y, Math.max(1, width - 8), Math.max(1, height));
        var pane = new WorkspaceScrollPane(panel);
        pane.setBounds(x, y, width, height);
        contentRoot.add(pane);
    }

    void addWorkspaceElement(TElement element) {
        contentRoot.add(element);
    }

    void buildRuleGroupsPage(int x, int y, int width, int height) {
        ruleGroups.init(x, y, width, height);
    }

    private void addIcon(TElement parent, String modId, int x, int y, int size) {
        Identifier icon = "minecraft".equals(modId)
            ? Identifier.withDefaultNamespace("textures/block/grass_block_side.png")
            : icons.icon(modId).orElse(null);
        if (icon != null) {
            var texture = new TTextureElement(icon);
            // Mod icons are dynamic textures; the vanilla icon is a direct resource texture.
            texture.modeProperty().set(TTextureElement.Mode.TEXTURE, WorkspaceDocumentInterface.class);
            texture.setBounds(x, y, size, size);
            parent.add(texture);
        } else {
            var fallback = WorkspaceStyle.label(
                parent,
                Component.literal(modId.substring(0, 1)
                    .toUpperCase()),
                x,
                y,
                size,
                size,
                WorkspaceStyle.ACCENT_COLOR
            );
            fallback.textAlignmentProperty()
                .set(CompassDirection.CENTER, WorkspaceDocumentInterface.class);
            fallback.textScaleProperty()
                .set(Math.max(1.0, size / 15.0), WorkspaceDocumentInterface.class);
        }
    }

    private static <T> List<WorkspaceStyle.Option<T>> options(T[] values, Function<T, Component> label) {
        return Arrays.stream(values)
            .map(value -> new WorkspaceStyle.Option<>(value, label.apply(value)))
            .toList();
    }

    private <T> void addDropdown(TElement parent, int x, int y, int width, List<WorkspaceStyle.Option<T>> options, T selected, Consumer<T> changed) {
        addDropdown(parent, x, y, width, options, selected, changed, null);
    }

    private <T> void addIconDropdown(TElement parent, int x, int y, int width, WorkspaceIcon.Kind kind, Component hint,
                                     List<WorkspaceStyle.Option<T>> options, T selected, Consumer<T> changed) {
        var icon = new WorkspaceIcon(kind);
        icon.setBounds(x, y, 20, 20);
        icon.hoverableProperty().set(true, WorkspaceDocumentInterface.class);
        icon.tooltipProperty().set(ignored -> TTooltip.of(hint), WorkspaceDocumentInterface.class);
        parent.add(icon);
        addDropdown(parent, x + 27, y, Math.max(1, width - 27), options, selected, changed, hint);
    }

    private <T> void addDropdown(TElement parent, int x, int y, int width, List<WorkspaceStyle.Option<T>> options,
                                 T selected, Consumer<T> changed, Component hint) {
        var initial = options.stream()
            .filter(option -> option.value()
                .equals(selected))
            .findFirst()
            .orElse(options.getFirst());
        var dropdown = new WorkspaceStyle.Dropdown<>(initial);
        dropdown.getEntries()
            .addAll(options);
        dropdown.setBounds(x, y, width, WorkspaceStyle.CONTROL_HEIGHT);
        if (hint != null) dropdown.tooltipProperty().set(ignored -> TTooltip.of(hint), WorkspaceDocumentInterface.class);
        dropdown.selectedEntryProperty()
            .addChangeListener((property, previous, current) -> {
                if (current != null) changed.accept(current.value());
            });
        parent.add(dropdown);
    }

    Component categoryLabel(RulePage page, String category) {
        if (category.equals(RuleBrowserModel.ALL_RULES)) return Component.translatable("carpet-gui.tab.all");
        if (category.equals(RuleBrowserModel.FAVORITES)) return Msg.tr("favorites");
        return category.equals(RuleBrowserModel.UNCATEGORIZED) ? Msg.tr("uncategorized") : page.categoryLabel(category);
    }

    private void selectPage(String id) {
        editor.selectPage(id);
        feedback = Component.empty();
        rebuildWorkspace();
    }

    void rebuildWorkspace() {
        if (ruleList != null) ruleList.cancelDrafts();
        screen.focusedElementProperty().set(null, WorkspaceDocumentInterface.class);
        clearAndInit();
    }

    void requestListRefresh() {
        requestListRefresh(false);
    }

    private void requestListRefresh(boolean reset) {
        listDirty = true;
        resetScroll |= reset;
    }

    private void expandAll(boolean expanded) {
        RulePage page = page();
        if (page == null) return;
        RuleBrowserModel.forPage(page.id())
            .expandAll(page, expanded);
        requestListRefresh();
    }

    void feedback(RuleView rule, RuleEditResult result) {
        feedback = rule.label()
            .copy()
            .append(": ")
            .append(result.message());
    }

    void showFeedback(Component message) {
        feedback = message;
    }

    void refreshRules() {
        editor.refresh();
    }

    int textWidth(String text) {
        return (int) Math.ceil(getClient().font.width(text) * WorkspaceStyle.TEXT_SCALE);
    }

    @Override
    protected void tickCallback() {
        if (observedNavigationRevision != editor.navigationRevision()) {
            rebuildWorkspace();
            return;
        }
        boolean poll = false;
        if (++refreshTicks >= 20) {
            refreshTicks = 0;
            editor.refresh();
            poll = true;
        }
        boolean sourceChanged = observedRevision != editor.revision();
        if (poll || sourceChanged) editor.refreshAllRules();
        RulePage page = page();
        if (modifiedFilter != null) {
            boolean configurationAvailable = ClientRuleConfigurations.ready() && page != null && !page.isVanilla();
            modifiedFilter.enabledProperty().set(configurationAvailable, WorkspaceDocumentInterface.class);
            if (!configurationAvailable && page != null && RuleBrowserModel.forPage(page.id()).modifiedOnly) {
                RuleBrowserModel.forPage(page.id()).modifiedOnly = false;
                modifiedFilter.checkedProperty().set(false, WorkspaceDocumentInterface.class);
                requestListRefresh(true);
            }
            if (savedDefaultFilter != null) {
                savedDefaultFilter.enabledProperty().set(configurationAvailable, WorkspaceDocumentInterface.class);
                if (!configurationAvailable && page != null && RuleBrowserModel.forPage(page.id()).savedDefaultOnly) {
                    RuleBrowserModel.forPage(page.id()).savedDefaultOnly = false;
                    savedDefaultFilter.checkedProperty().set(false, WorkspaceDocumentInterface.class);
                    requestListRefresh(true);
                }
            }
        }
        if (ruleList != null) {
            // Live refresh is independent of the backend's observer support.
            ruleList.refreshRows();
            boolean overlay = findChild(element -> element instanceof TContextMenu, false).isPresent();
            boolean dragging = findChild(
                element -> element instanceof TClickableWidget clickable && clickable.pressedProperty()
                    .getZ(), true
            ).isPresent();
            boolean textFocused = screen.focusedElementProperty().get() instanceof NativeTextInput;
            if ((poll || sourceChanged) && page != null && !observedCategories.equals(RuleBrowserModel.forPage(page.id()).categories(page))
                && !ruleList.interacting() && !textFocused && !overlay && !dragging) {
                rebuildWorkspace();
                return;
            }
            if ((listDirty || sourceChanged || refreshTicks % 5 == 0 && ruleList.changed()) && !ruleList.interacting() && !overlay && !dragging) {
                ruleList.rebuild(resetScroll);
                listDirty = false;
                resetScroll = false;
                observedRevision = editor.revision();
            }
            if (counts != null && page != null) {
                counts.setText(ruleList.countsText());
            }
        }
        if (editor.isSelected(RuleGroupWorkspace.ID)) ruleGroups.tick();
        if (notice != null) {
            notice.setText(feedback);
        }
        if (homeCounts != null) homeCounts.setText(Component.translatable("carpet-gui.workspace.overview_counts",
            editor.sourcePages().stream().filter(candidate -> !candidate.isVanilla()).count(),
            editor.sourcePages().stream().mapToInt(candidate -> candidate.rules().size()).sum()));
        if (connectionStatus != null) connectionStatus.setText(Msg.tr(getClient().getConnection() == null ? "offline" : "connected"));
        layoutHomeOverview();
    }

    @Override
    public boolean inputCallback(TInputContext.InputDiscoveryPhase phase, TInputContext context) {
        if (phase == TInputContext.InputDiscoveryPhase.BROADCAST && context.getInputType() == TInputContext.InputType.MOUSE_PRESS &&
            //#if MC >= 260300
            context.getMouseButton() == SDL_BUTTON_LEFT && ruleList != null &&
            //#else
            //$$ context.getMouseButton() == GLFW_MOUSE_BUTTON_LEFT && ruleList != null &&
            //#endif
            findChild(element -> element instanceof TContextMenu, false).isEmpty()) {
            ruleList.beforeMousePress(context.getMouseX(), context.getMouseY());
        }
        if (phase == TInputContext.InputDiscoveryPhase.BROADCAST && context.getInputType() == TInputContext.InputType.MOUSE_PRESS &&
            //#if MC >= 260300
            context.getMouseButton() == SDL_BUTTON_LEFT && editor.isSelected(RuleGroupWorkspace.ID) &&
            //#else
            //$$ context.getMouseButton() == GLFW_MOUSE_BUTTON_LEFT && editor.isSelected(RuleGroupWorkspace.ID) &&
            //#endif
            findChild(element -> element instanceof TContextMenu, false).isEmpty()) {
            ruleGroups.beforeMousePress(context.getMouseX(), context.getMouseY());
        }
        return super.inputCallback(phase, context);
    }

    @Override
    public void renderCallback(TGuiGraphics graphics) {
        var bounds = getBounds();
        graphics.fillColor(0, 0, bounds.width, bounds.height, WorkspaceStyle.BACKDROP_COLOR);
    }



    private static String version() {
        return FabricLoader.getInstance()
            .getModContainer("carpet-gui")
            .map(mod -> mod.getMetadata()
                .getVersion()
                .getFriendlyString())
            .orElse("?");
    }
}
//#endif


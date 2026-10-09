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
import com.thecsdev.commonmc.api.client.gui.screen.ILastScreenProvider;
import com.thecsdev.commonmc.api.client.gui.screen.TScreenPlus;
import com.thecsdev.commonmc.api.client.gui.screen.TScreenWrapper;
import com.thecsdev.commonmc.api.client.gui.tooltip.TTooltip;
import com.thecsdev.commonmc.api.client.gui.util.TInputContext;
import com.thecsdev.commonmc.api.client.gui.widget.TClickableWidget;
import com.thecsdev.commonmc.api.client.gui.widget.TCheckboxWidget;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleEditResult;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RulePage;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleSource;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleView;
import io.github.piscescup.fabricmc.carpetgui.network.ClientRuleConfigurations;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
//#if MC >= 260300
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.PreeditEvent;
//#endif
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

//#if MC >= 260300
import static org.lwjgl.sdl.SDLMouse.SDL_BUTTON_LEFT;
//#else
//$$ import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
//#endif

/**
 * TCDCommons-powered Home/mod workspace. Editing and networking remain backend capabilities.
 */
public final class CarpetWorkspaceScreen
    extends TScreenPlus
    implements ILastScreenProvider
{
    private final Screen parent;
    private final RuleSource source;
    private final List<? extends RulePage> sourcePages;
    private final List<? extends RulePage> pages;
    private final AllRulesPage allRules;
    private final RuleGroupWorkspace ruleGroups;
    private final Set<String> openedTabs = new LinkedHashSet<>();
    private final ModIconStore icons = new ModIconStore();
    private String selectedId;
    private RuleListPanel ruleList;
    private TLabelElement notice;
    private TLabelElement counts;
    private TLabelElement homeCounts;
    private TLabelElement connectionStatus;
    private TLabelElement homeHelpHeading;
    private TLabelElement homeHelpBody;
    private TPanelElement tabs;
    private TCheckboxWidget modifiedFilter;
    private TCheckboxWidget savedDefaultFilter;
    private Component feedback = Component.empty();
    private boolean listDirty;
    private boolean resetScroll;
    private int refreshTicks;
    private long observedRevision;
    private List<String> observedCategories = List.of();

    public CarpetWorkspaceScreen(Screen parent, RuleSource source, String initialPageId) {
        super(Component.translatable("carpet-gui.screen.title"));
        this.parent = parent;
        this.source = source;
        sourcePages = List.copyOf(source.pages());
        allRules = new AllRulesPage(sourcePages);
        var navigationPages = new ArrayList<RulePage>(sourcePages.size() + 1);
        navigationPages.add(allRules);
        navigationPages.addAll(sourcePages);
        pages = List.copyOf(navigationPages);
        ruleGroups = new RuleGroupWorkspace(this, sourcePages);
        pages.forEach(page -> {
            RuleBrowserModel.forPage(page.id())
                .invalidateSearch();
        });
        openedTabs.add(AllRulesPage.ID);
        selectedId = RuleGroupWorkspace.ID.equals(initialPageId) || pages.stream()
            .anyMatch(page -> page.id().equals(initialPageId)) ? initialPageId : null;
        if (selectedId != null) openedTabs.add(selectedId);
    }

    @Override
    public Screen getLastScreen() {
        return parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isAllowingInGameHud() {
        return false;
    }

    @Override
    protected @NonNull TScreenWrapper<?> createWrapperScreen() {
        return new Wrapper(this);
    }

    @Override
    protected void openCallback() {
        source.refresh();
    }

    @Override
    protected void closeCallback() {
        if (ruleList != null) ruleList.cancelDrafts();
        ruleGroups.cancelDrafts();
        icons.close();
    }

    @Override
    public void close() {
        if (ruleList != null) ruleList.cancelDrafts();
        ruleGroups.cancelDrafts();
        focusedElementProperty().set(null, CarpetWorkspaceScreen.class);
        super.close();
    }

    private RulePage page() {
        return pages.stream()
            .filter(page -> page.id()
                .equals(selectedId))
            .findFirst()
            .orElse(null);
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
        var frame = new TPanelElement.Paintable(WorkspaceStyle.FRAME, WorkspaceStyle.BORDER, WorkspaceStyle.BORDER);
        frame.setBounds(x, WorkspaceStyle.TAB_Y - 1, width, bodyY + bodyHeight - WorkspaceStyle.TAB_Y + 1);
        frame.hoverableProperty().set(false, CarpetWorkspaceScreen.class);
        frame.focusableProperty().set(false, CarpetWorkspaceScreen.class);
        add(frame);
        initMenus(x, width);
        initTabs(x, width);
        RulePage page = page();
        if (RuleGroupWorkspace.ID.equals(selectedId)) {
            ruleGroups.init(x, bodyY, width, bodyHeight);
        } else if (page == null) {
            initHome(x, bodyY, width, bodyHeight);
        } else {
            initRules(page, x, bodyY, width, bodyHeight);
        }
        notice = WorkspaceStyle.label(this, Component.empty(), x + 4, screen.height - 18, width - 8, 14, WorkspaceStyle.MUTED);
        notice.wrapTextProperty()
            .set(false, CarpetWorkspaceScreen.class);
        observedRevision = source.revision();
        listDirty = false;
    }

    private void initMenus(int x, int width) {
        var bar = new WorkspaceStaticPanel(0x45000000, WorkspaceStyle.BORDER, WorkspaceStyle.BORDER);
        bar.setBounds(x, 0, width, WorkspaceStyle.MENU_HEIGHT);
        add(bar);
        addMenu(
            bar, x + 1, "file", List.of(
                new MenuAction(tr("home"), () -> selectPage(null)),
                new MenuAction(
                    tr("refresh"), () -> {
                    source.refresh();
                    requestListRefresh();
                }
                )
            )
        );
        var view = new WorkspaceStyle.ChromeButton(tr("view"), () -> showViewMenu(x + 37));
        view.setBounds(x + 37, 1, 36, WorkspaceStyle.MENU_HEIGHT - 2);
        bar.add(view);
        addMenu(
            bar, x + 73, "about", List.of(
                new MenuAction(Component.literal("Carpet GUI " + version()), () -> selectPage(null)),
                new MenuAction(tr("help"), () -> selectPage(null))
            )
        );
    }

    private void addMenu(TElement parent, int x, String name, List<MenuAction> actions) {
        var button = new WorkspaceStyle.ChromeButton(tr(name), () -> showMenu(x, actions));
        button.setBounds(x, 1, 36, WorkspaceStyle.MENU_HEIGHT - 2);
        parent.add(button);
    }

    private void showViewMenu(int x) {
        var entries = new ArrayList<WorkspaceNavigationMenu.Entry>();
        entries.add(new WorkspaceNavigationMenu.Entry("carpet-gui", tr("home"), selectedId == null, !pages.isEmpty(), () -> selectPage(null)));
        entries.add(new WorkspaceNavigationMenu.Entry("carpet-gui", tr("rule_groups"), RuleGroupWorkspace.ID.equals(selectedId), true,
            () -> selectPage(RuleGroupWorkspace.ID)));
        for (RulePage page : pages) {
            entries.add(new WorkspaceNavigationMenu.Entry(iconId(page), page.title(), page.id().equals(selectedId), false, () -> selectPage(page.id())));
        }
        var menu = new WorkspaceNavigationMenu(entries, this::addIcon);
        int widestTitle = entries.stream().mapToInt(entry -> (int) Math.ceil(getClient().font.width(entry.title()) * WorkspaceStyle.TEXT_SCALE)).max().orElse(0);
        int width = Math.min(getBounds().width, Math.max(100, widestTitle + 38));
        int height = Math.min(Math.max(1, getBounds().height - WorkspaceStyle.MENU_HEIGHT - 4), menu.contentHeight() + WorkspaceNavigationMenu.PADDING * 2);
        menu.setBounds(Math.clamp(x, 0, Math.max(0, getBounds().width - width)), WorkspaceStyle.MENU_HEIGHT, width, height);
        add(menu);
        menu.clearAndInit();
    }

    private void showMenu(int x, List<MenuAction> actions) {
        var entries = actions.stream().map(action -> new WorkspaceNavigationMenu.Entry(null, action.label(), false, false, action.action())).toList();
        var menu = new WorkspaceNavigationMenu(entries, null);
        int widestTitle = actions.stream().mapToInt(action -> (int) Math.ceil(getClient().font.width(action.label()) * WorkspaceStyle.TEXT_SCALE)).max().orElse(0);
        int width = Math.min(getBounds().width, Math.max(100, widestTitle + 28));
        int height = Math.min(Math.max(1, getBounds().height - WorkspaceStyle.MENU_HEIGHT - 4), menu.contentHeight() + WorkspaceNavigationMenu.PADDING * 2);
        menu.setBounds(Math.clamp(x, 0, Math.max(0, getBounds().width - width)), WorkspaceStyle.MENU_HEIGHT, width, height);
        add(menu);
        menu.clearAndInit();
    }

    private void initTabs(int x, int width) {
        tabs = new TPanelElement.Paintable(0x35000000, 0, 0) {
            @Override
            public boolean inputCallback(TInputContext.InputDiscoveryPhase phase, TInputContext context) {
                if (phase == TInputContext.InputDiscoveryPhase.MAIN && context.getInputType() == TInputContext.InputType.MOUSE_SCROLL) {
                    scroll((int) (context.getScrollY() * 35 - context.getScrollX() * 35), 0);
                    return true;
                }
                return super.inputCallback(phase, context);
            }
        };
        tabs.setBounds(x + 1, WorkspaceStyle.TAB_Y, Math.max(1, width - 2), WorkspaceStyle.TAB_HEIGHT);
        add(tabs);
        int tabX = x + 1;
        tabX += addTab(tabX, null, "carpet-gui", tr("home"), 56, false);
        if (openedTabs.contains(RuleGroupWorkspace.ID)) {
            int featureWidth = Math.clamp((int) Math.ceil(getClient().font.width(tr("rule_groups")) * WorkspaceStyle.TEXT_SCALE) + 35, 90, 190);
            tabX += addTab(tabX, RuleGroupWorkspace.ID, "carpet-gui", tr("rule_groups"), featureWidth, true);
        }
        for (RulePage page : pages) {
            if (!openedTabs.contains(page.id())) continue;
            int tabWidth = Math.clamp((int) Math.ceil(getClient().font.width(page.title()) * WorkspaceStyle.TEXT_SCALE) + 35, 80, 190);
            tabX += addTab(tabX, page.id(), iconId(page), page.title(), tabWidth, true);
        }
        // Bring the selected tab into the viewport after resize/switch/reopening a closed tab.
        for (TElement tab : tabs) {
            if (tab instanceof WorkspaceTab workspaceTab && workspaceTab.selected) {
                var bounds = tab.getBounds();
                if (bounds.endX > x + width) {
                    tabs.scroll(x + width - bounds.endX, 0);
                } else if (bounds.x < x) tabs.scroll(x - bounds.x, 0);
                break;
            }
        }
    }

    private int addTab(int x, String id, String iconId, Component title, int width, boolean closable) {
        var tab = new WorkspaceTab(id, iconId, closable, title, () -> selectPage(id));
        tab.setBounds(x, WorkspaceStyle.TAB_Y, width, WorkspaceStyle.TAB_HEIGHT);
        tab.selected = id == null ? selectedId == null : id.equals(selectedId);
        tab.setSelected(tab.selected);
        tabs.add(tab);
        return width;
    }

    private void initRules(RulePage page, int x, int y, int width, int height) {
        RuleBrowserModel model = RuleBrowserModel.forPage(page.id());
        observedCategories = model.categories(page);
        if (!model.category.isEmpty() && !model.category.equals(RuleBrowserModel.FAVORITES)
            && !model.categories(page).contains(model.category)) {
            model.category = "";
        }
        int sidebarWidth = Math.clamp(width * 30 / 100, Math.min(120, width / 3), 235);
        var sidebar = new WorkspacePanel(12);
        addWorkspacePane(sidebar, x, y, sidebarWidth, height);
        initSidebar(sidebar, page, model);
        int listX = x + sidebarWidth;
        int listWidth = Math.max(1, width - sidebarWidth);
        counts = WorkspaceStyle.label(this, Component.empty(), listX + 10, y + 8, listWidth - 20, 18, WorkspaceStyle.MUTED);
        ruleList = new RuleListPanel(this, page, model);
        addWorkspacePane(ruleList, listX, y + 23, listWidth, Math.max(1, height - 23));
    }

    private void initSidebar(TPanelElement sidebar, RulePage page, RuleBrowserModel model) {
        var bounds = sidebar.getBounds();
        int x = bounds.x + 9, width = Math.max(1, bounds.width - 18), y = bounds.y + 9;
        var filterTitle = WorkspaceStyle.label(sidebar, tr("filters"), x, y, width, 18, WorkspaceStyle.TEXT);
        filterTitle.textAlignmentProperty().set(CompassDirection.CENTER, CarpetWorkspaceScreen.class);
        y += 23;
        List<WorkspaceStyle.Option<String>> categories = new ArrayList<>();
        categories.add(new WorkspaceStyle.Option<>("", tr("all_categories")));
        model.categories(page)
            .forEach(category -> categories.add(new WorkspaceStyle.Option<>(category, categoryLabel(page, category))));
        WorkspaceStyle.Option<String> lastCategory = categories.removeLast();
        categories.add(new WorkspaceStyle.Option<>(lastCategory.value(), lastCategory.label(), true));
        categories.add(new WorkspaceStyle.Option<>(RuleBrowserModel.FAVORITES, tr("favorites")));
        addDropdown(
            sidebar, x, y, width, categories, model.category, category -> {
                model.category = category;
                requestListRefresh(true);
            }
        );
        y += 25;
        var search = new NativeTextInput(
            tr("search"), model.query, query -> {
            model.query = query;
            requestListRefresh(true);
        },
            () -> {
            }, () -> focusedElementProperty().set(null, CarpetWorkspaceScreen.class)
        );
        search.setBounds(x, y, width, WorkspaceStyle.CONTROL_HEIGHT);
        search.tooltipProperty()
            .set(ignored -> TTooltip.of(tr("search_hint")), CarpetWorkspaceScreen.class);
        sidebar.add(search);
        y += 25;
        if (!ClientRuleConfigurations.ready()) model.modifiedOnly = false;
        modifiedFilter = new TCheckboxWidget(model.modifiedOnly);
        modifiedFilter.setBounds(x, y, WorkspaceStyle.CONTROL_HEIGHT, WorkspaceStyle.CONTROL_HEIGHT);
        modifiedFilter.enabledProperty().set(ClientRuleConfigurations.ready() && !page.isVanilla(), CarpetWorkspaceScreen.class);
        modifiedFilter.checkedProperty().addChangeListener((property, previous, checked) -> {
            model.modifiedOnly = checked;
            requestListRefresh(true);
        });
        Component modifiedHint = tr("modified_only_hint").copy()
            .append("\n")
            .append(tr("modified_only_server_required").copy().withStyle(ChatFormatting.GOLD));
        modifiedFilter.tooltipProperty().set(ignored -> TTooltip.of(modifiedHint), CarpetWorkspaceScreen.class);
        sidebar.add(modifiedFilter);
        var modifiedLabel = WorkspaceStyle.label(sidebar, tr("modified_only"), x + 27, y + 6, width - 27, 12, WorkspaceStyle.TEXT);
        modifiedLabel.tooltipProperty().set(ignored -> TTooltip.of(modifiedHint), CarpetWorkspaceScreen.class);
        y += 27;
        var initialDifference = new TCheckboxWidget(model.initialDifferenceOnly);
        initialDifference.setBounds(x, y, WorkspaceStyle.CONTROL_HEIGHT, WorkspaceStyle.CONTROL_HEIGHT);
        initialDifference.checkedProperty().addChangeListener((property, previous, checked) -> {
            model.initialDifferenceOnly = checked;
            requestListRefresh(true);
        });
        initialDifference.tooltipProperty().set(
            ignored -> TTooltip.of(tr("initial_difference_hint")), CarpetWorkspaceScreen.class
        );
        sidebar.add(initialDifference);
        var initialDifferenceLabel = WorkspaceStyle.label(
            sidebar, tr("initial_difference_only"), x + 27, y + 6, width - 27, 12, WorkspaceStyle.TEXT
        );
        initialDifferenceLabel.tooltipProperty().set(
            ignored -> TTooltip.of(tr("initial_difference_hint")), CarpetWorkspaceScreen.class
        );
        y += 27;
        if (!ClientRuleConfigurations.ready()) model.savedDefaultOnly = false;
        savedDefaultFilter = new TCheckboxWidget(model.savedDefaultOnly);
        savedDefaultFilter.setBounds(x, y, WorkspaceStyle.CONTROL_HEIGHT, WorkspaceStyle.CONTROL_HEIGHT);
        savedDefaultFilter.enabledProperty().set(ClientRuleConfigurations.ready() && !page.isVanilla(), CarpetWorkspaceScreen.class);
        savedDefaultFilter.checkedProperty().addChangeListener((property, previous, checked) -> {
            model.savedDefaultOnly = checked;
            requestListRefresh(true);
        });
        Component savedDefaultHint = tr("saved_default_only_hint").copy()
            .append("\n")
            .append(tr("modified_only_server_required").copy().withStyle(ChatFormatting.GOLD));
        savedDefaultFilter.tooltipProperty().set(ignored -> TTooltip.of(savedDefaultHint), CarpetWorkspaceScreen.class);
        sidebar.add(savedDefaultFilter);
        var savedDefaultLabel = WorkspaceStyle.label(
            sidebar, tr("saved_default_only"), x + 27, y + 6, width - 27, 12, WorkspaceStyle.TEXT
        );
        savedDefaultLabel.tooltipProperty().set(ignored -> TTooltip.of(savedDefaultHint), CarpetWorkspaceScreen.class);
        y += 27;
        addIconDropdown(
            sidebar,
            x,
            y,
            width,
            WorkspaceIcon.Kind.SORT,
            tr("sort"),
            options(
                RuleBrowserModel.Sort.values(),
                value -> tr("sort." + value.name()
                    .toLowerCase(Locale.ROOT))
            ),
            model.sort,
            sort -> {
                model.sort = sort;
                requestListRefresh(true);
            }
        );
        y += 27;
        addIconDropdown(sidebar, x, y, width, WorkspaceIcon.Kind.GROUP, tr("grouping"),
            options(RuleBrowserModel.Grouping.values(), value -> tr("grouping." + value.name().toLowerCase(Locale.ROOT))),
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
            tr("distance"),
            options(
                RuleBrowserModel.Distance.values(),
                value -> tr("distance." + value.name()
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
            tr("time"),
            options(
                RuleBrowserModel.Time.values(),
                value -> tr("time." + value.name()
                    .toLowerCase(Locale.ROOT))
            ),
            model.time,
            value -> model.time = value
        );
        y += 29;
        int gap = 4;
        int expandWidth = Math.max(1, (width - gap) / 2);
        int collapseWidth = Math.max(1, width - gap - expandWidth);
        var expand = new WorkspaceStyle.ControlButton(Component.translatable("carpet-gui.expand_all"), () -> expandAll(true));
        expand.setBounds(x, y, expandWidth, WorkspaceStyle.CONTROL_HEIGHT);
        expand.getLabel().textScaleProperty().set(Math.min(WorkspaceStyle.TEXT_SCALE,
            Math.max(1, expandWidth - 8) / (double) Math.max(1, getClient().font.width(expand.getLabel().getText()))), CarpetWorkspaceScreen.class);
        sidebar.add(expand);
        var collapse = new WorkspaceStyle.ControlButton(Component.translatable("carpet-gui.collapse_all"), () -> expandAll(false));
        collapse.setBounds(x + expandWidth + gap, y, collapseWidth, WorkspaceStyle.CONTROL_HEIGHT);
        collapse.getLabel().textScaleProperty().set(Math.min(WorkspaceStyle.TEXT_SCALE,
            Math.max(1, collapseWidth - 8) / (double) Math.max(1, getClient().font.width(collapse.getLabel().getText()))), CarpetWorkspaceScreen.class);
        sidebar.add(collapse);
    }

    private void initHome(int x, int y, int width, int height) {
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
        WorkspaceStyle.label(left, Component.literal("Carpet GUI"), contentX + 38, nextY + 1, contentWidth - 38, 13, WorkspaceStyle.ACCENT);
        WorkspaceStyle.label(left, Component.literal(version()), contentX + 38, nextY + 18, contentWidth - 38, 13, WorkspaceStyle.MUTED);
        addIcon(left, "carpet-gui", contentX, nextY, 30);
        nextY += 51;
        WorkspaceStyle.label(left, tr("quick_access"), contentX, nextY, contentWidth, 16, WorkspaceStyle.ACCENT);
        nextY += 25;
        int columns = Math.max(1, contentWidth / 42);
        int quickAccessHeight = Math.max(1, (sourcePages.size() + columns - 1) / columns) * 42;
        addHomeCard(left, contentX - 4, nextY - 4, contentWidth + 8, quickAccessHeight + 2);
        for (int index = 0; index < sourcePages.size(); index++) {
            RulePage page = sourcePages.get(index);
            int tileX = contentX + (index % columns) * 42, tileY = nextY + (index / columns) * 42;
            var tile = new WorkspaceStyle.IconButton(() -> selectPage(page.id())) {
                @Override protected void initCallback() {
                    super.initCallback();
                    var bounds = getBounds();
                    addIcon(this, page.id(), bounds.x + 5, bounds.y + 5, 26);
                }
            };
            tile.setBounds(tileX, tileY, 36, 36);
            tile.tooltipProperty()
                .set(ignored -> TTooltip.of(page.title()), CarpetWorkspaceScreen.class);
            left.add(tile);
        }
        nextY += quickAccessHeight + 16;
        WorkspaceStyle.label(left, tr("features"), contentX, nextY, contentWidth, 16, WorkspaceStyle.ACCENT);
        nextY += 22;
        addHomeCard(left, contentX - 4, nextY - 4, contentWidth + 8, 46);
        var groupsFeature = new WorkspaceStyle.Button(tr("rule_groups"), () -> selectPage(RuleGroupWorkspace.ID)) {
            @Override protected void initCallback() {
                super.initCallback();
                var bounds = getBounds();
                getLabel().setBounds(bounds.x + 39, bounds.y + 4, Math.max(1, bounds.width - 45), 14);
                getLabel().textAlignmentProperty().set(CompassDirection.WEST, CarpetWorkspaceScreen.class);
                var description = WorkspaceStyle.label(this, tr("rule_groups_home"), bounds.x + 39, bounds.y + 21,
                    Math.max(1, bounds.width - 45), 11, WorkspaceStyle.MUTED);
                description.textScaleProperty().set(WorkspaceStyle.SMALL_TEXT_SCALE, CarpetWorkspaceScreen.class);
                description.wrapTextProperty().set(false, CarpetWorkspaceScreen.class);
                addIcon(this, "carpet-gui", bounds.x + 6, bounds.y + 7, 27);
            }
        };
        groupsFeature.setBounds(contentX, nextY, contentWidth, 38);
        groupsFeature.tooltipProperty().set(ignored -> TTooltip.of(tr("rule_groups_home")), CarpetWorkspaceScreen.class);
        left.add(groupsFeature);
        nextY += 56;
        WorkspaceStyle.label(left, tr("news"), contentX, nextY, contentWidth, 18, WorkspaceStyle.ACCENT);
        // nextY = addParagraph(left, tr("news_intro"), contentX, nextY + 22, contentWidth, WorkspaceStyle.MUTED) + 16;
        // nextY = addParagraph(left, tr("news_workspace"), contentX, nextY, contentWidth, WorkspaceStyle.TEXT) + 16;
        // addParagraph(left, tr("news_addons"), contentX, nextY, contentWidth, WorkspaceStyle.TEXT);
        var rightBounds = right.getBounds();
        int rightX = rightBounds.x + 12, rightWidth = Math.max(1, rightBounds.width - 24), rightY = y + 12;
        WorkspaceStyle.label(right, tr("overview"), rightX, rightY, rightWidth, 18, WorkspaceStyle.ACCENT);
        homeCounts = WorkspaceStyle.label(
            right, Component.translatable(
                "carpet-gui.workspace.overview_counts",
                sourcePages.stream()
                    .filter(page -> !page.isVanilla())
                    .count(),
                sourcePages.stream()
                    .mapToInt(page -> page.rules()
                        .size())
                    .sum()
            ), rightX, rightY + 26, rightWidth, 40, WorkspaceStyle.TEXT
        );
        connectionStatus = WorkspaceStyle.label(
            right,
            tr(getClient().getConnection() == null ? "offline" : "connected"),
            rightX,
            rightY + 72,
            rightWidth,
            33,
            WorkspaceStyle.MUTED
        );
        homeHelpHeading = WorkspaceStyle.label(right, tr("help"), rightX, rightY + 125, rightWidth, 18, WorkspaceStyle.ACCENT);
        homeHelpBody = WorkspaceStyle.label(right, tr("help_body"), rightX, rightY + 152, rightWidth, 10, WorkspaceStyle.TEXT);
        // Fit height within the available column, rather than expanding labels to their full text width.
        homeCounts.wrapTextProperty().set(true, CarpetWorkspaceScreen.class);
        connectionStatus.wrapTextProperty().set(true, CarpetWorkspaceScreen.class);
        homeHelpBody.wrapTextProperty().set(true, CarpetWorkspaceScreen.class);
        layoutHomeOverview();
    }

    private void addHomeCard(TElement parent, int x, int y, int width, int height) {
        var card = new TPanelElement.Paintable(WorkspaceStyle.CARD, 0, 0);
        card.setBounds(x, y, width, height);
        card.hoverableProperty().set(false, CarpetWorkspaceScreen.class);
        card.focusableProperty().set(false, CarpetWorkspaceScreen.class);
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
        label.wrapTextProperty().set(true, CarpetWorkspaceScreen.class);
        label.setBoundsToFitText(x, y, Math.max(1, width));
        return label.getBounds().endY;
    }

    void addWorkspacePane(TPanelElement panel, int x, int y, int width, int height) {
        // Child initialization is recursive and happens after this screen's init callback returns.
        panel.setBounds(x, y, Math.max(1, width - 8), Math.max(1, height));
        var pane = new WorkspaceScrollPane(panel);
        pane.setBounds(x, y, width, height);
        add(pane);
    }

    void addWorkspaceElement(TElement element) {
        add(element);
    }

    private void addIcon(TElement parent, String modId, int x, int y, int size) {
        Identifier icon = "minecraft".equals(modId)
            ? Identifier.withDefaultNamespace("textures/block/grass_block_side.png")
            : icons.icon(modId).orElse(null);
        if (icon != null) {
            var texture = new TTextureElement(icon);
            // Mod icons are dynamic textures; the vanilla icon is a direct resource texture.
            texture.modeProperty().set(TTextureElement.Mode.TEXTURE, CarpetWorkspaceScreen.class);
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
                WorkspaceStyle.ACCENT
            );
            fallback.textAlignmentProperty()
                .set(CompassDirection.CENTER, CarpetWorkspaceScreen.class);
            fallback.textScaleProperty()
                .set(Math.max(1.0, size / 15.0), CarpetWorkspaceScreen.class);
        }
    }

    private static String iconId(RulePage page) {
        return page instanceof AllRulesPage ? "carpet-gui" : page.id();
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
        icon.hoverableProperty().set(true, CarpetWorkspaceScreen.class);
        icon.tooltipProperty().set(ignored -> TTooltip.of(hint), CarpetWorkspaceScreen.class);
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
        if (hint != null) dropdown.tooltipProperty().set(ignored -> TTooltip.of(hint), CarpetWorkspaceScreen.class);
        dropdown.selectedEntryProperty()
            .addChangeListener((property, previous, current) -> {
                if (current != null) changed.accept(current.value());
            });
        parent.add(dropdown);
    }

    Component categoryLabel(RulePage page, String category) {
        if (category.equals(RuleBrowserModel.ALL_RULES)) return Component.translatable("carpet-gui.tab.all");
        if (category.equals(RuleBrowserModel.FAVORITES)) return tr("favorites");
        return category.equals(RuleBrowserModel.UNCATEGORIZED) ? tr("uncategorized") : page.categoryLabel(category);
    }

    private void selectPage(String id) {
        if (id != null) openedTabs.add(id);
        selectedId = id;
        feedback = Component.empty();
        rebuildWorkspace();
    }

    void rebuildWorkspace() {
        if (ruleList != null) ruleList.cancelDrafts();
        focusedElementProperty().set(null, CarpetWorkspaceScreen.class);
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
        source.refresh();
    }

    int textWidth(String text) {
        return (int) Math.ceil(getClient().font.width(text) * WorkspaceStyle.TEXT_SCALE);
    }

    @Override
    protected void tickCallback() {
        boolean poll = false;
        if (++refreshTicks >= 20) {
            refreshTicks = 0;
            source.refresh();
            poll = true;
        }
        boolean sourceChanged = observedRevision != source.revision();
        if (poll || sourceChanged) allRules.refreshIndex();
        RulePage page = page();
        if (modifiedFilter != null) {
            boolean configurationAvailable = ClientRuleConfigurations.ready() && page != null && !page.isVanilla();
            modifiedFilter.enabledProperty().set(configurationAvailable, CarpetWorkspaceScreen.class);
            if (!configurationAvailable && page != null && RuleBrowserModel.forPage(page.id()).modifiedOnly) {
                RuleBrowserModel.forPage(page.id()).modifiedOnly = false;
                modifiedFilter.checkedProperty().set(false, CarpetWorkspaceScreen.class);
                requestListRefresh(true);
            }
            if (savedDefaultFilter != null) {
                savedDefaultFilter.enabledProperty().set(configurationAvailable, CarpetWorkspaceScreen.class);
                if (!configurationAvailable && page != null && RuleBrowserModel.forPage(page.id()).savedDefaultOnly) {
                    RuleBrowserModel.forPage(page.id()).savedDefaultOnly = false;
                    savedDefaultFilter.checkedProperty().set(false, CarpetWorkspaceScreen.class);
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
            boolean textFocused = focusedElementProperty().get() instanceof NativeTextInput;
            if ((poll || sourceChanged) && page != null && !observedCategories.equals(RuleBrowserModel.forPage(page.id()).categories(page))
                && !ruleList.interacting() && !textFocused && !overlay && !dragging) {
                rebuildWorkspace();
                return;
            }
            if ((listDirty || sourceChanged || refreshTicks % 5 == 0 && ruleList.changed()) && !ruleList.interacting() && !overlay && !dragging) {
                ruleList.rebuild(resetScroll);
                listDirty = false;
                resetScroll = false;
                observedRevision = source.revision();
            }
            if (counts != null && page != null) {
                counts.setText(ruleList.countsText());
            }
        }
        if (RuleGroupWorkspace.ID.equals(selectedId)) ruleGroups.tick();
        if (notice != null) {
            notice.setText(feedback);
        }
        if (homeCounts != null) homeCounts.setText(Component.translatable("carpet-gui.workspace.overview_counts",
            sourcePages.stream().filter(candidate -> !candidate.isVanilla()).count(),
            sourcePages.stream().mapToInt(candidate -> candidate.rules().size()).sum()));
        if (connectionStatus != null) connectionStatus.setText(tr(getClient().getConnection() == null ? "offline" : "connected"));
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
            context.getMouseButton() == SDL_BUTTON_LEFT && RuleGroupWorkspace.ID.equals(selectedId) &&
            //#else
            //$$ context.getMouseButton() == GLFW_MOUSE_BUTTON_LEFT && RuleGroupWorkspace.ID.equals(selectedId) &&
            //#endif
            findChild(element -> element instanceof TContextMenu, false).isEmpty()) {
            ruleGroups.beforeMousePress(context.getMouseX(), context.getMouseY());
        }
        return super.inputCallback(phase, context);
    }

    @Override
    public void renderCallback(TGuiGraphics graphics) {
        var bounds = getBounds();
        graphics.fillColor(0, 0, bounds.width, bounds.height, WorkspaceStyle.BACKDROP);
    }

    private static Component tr(String key) {
        return Component.translatable("carpet-gui.workspace." + key);
    }

    private static String version() {
        return FabricLoader.getInstance()
            .getModContainer("carpet-gui")
            .map(mod -> mod.getMetadata()
                .getVersion()
                .getFriendlyString())
            .orElse("?");
    }

    private record MenuAction(
        Component label,
        Runnable action
    )
    {
    }

    private final class WorkspaceTab
        extends WorkspaceStyle.ChromeButton
    {
        private final String id;
        private final String iconId;
        private final boolean closable;
        private boolean selected;

        WorkspaceTab(String id, String iconId, boolean closable, Component title, Runnable action) {
            super(title, action);
            this.id = id;
            this.iconId = iconId;
            this.closable = closable;
        }

        @Override
        protected void initCallback() {
            super.initCallback();
            var bounds = getBounds();
            getLabel().setBounds(bounds.x + 19, bounds.y + 2, Math.max(1, bounds.width - (closable ? 34 : 23)), bounds.height - 4);
            getLabel().textAlignmentProperty()
                .set(CompassDirection.WEST, WorkspaceTab.class);
            addIcon(this, iconId, bounds.x + 5, bounds.y + 4, 10);
            if (closable) {
                var close = new WorkspaceStyle.ChromeButton(
                    Component.literal("×"), () -> {
                        openedTabs.remove(id);
                        if (id.equals(selectedId)) {
                            selectedId = null;
                        }
                        rebuildWorkspace();
                    }
                );
                close.setBounds(bounds.endX - 15, bounds.y + 2, 13, bounds.height - 4);
                close.tooltipProperty()
                    .set(ignored -> TTooltip.of(tr("close_tab")), WorkspaceTab.class);
                add(close);
            }
            tooltipProperty().set(ignored -> TTooltip.of(getLabel().getText()), WorkspaceTab.class);
        }
    }

    /**
     * TCD's stock wrapper does not forward native Unicode/IME events to its custom tree.
     */
    private static final class Wrapper
        extends TScreenWrapper<CarpetWorkspaceScreen>
    {
        Wrapper(CarpetWorkspaceScreen target) {
            super(target);
        }

        //#if MC >= 260300
        @Override
        public boolean charTyped(CharacterEvent event) {
            TElement focused = getTargetTScreen().focusedElementProperty()
                .get();
            return focused instanceof NativeTextInput input ? input.charTyped(event) : super.charTyped(event);
        }

        @Override
        public boolean preeditUpdated(PreeditEvent event) {
            TElement focused = getTargetTScreen().focusedElementProperty()
                .get();
            return focused instanceof NativeTextInput input ? input.preeditUpdated(event) : super.preeditUpdated(event);
        }

        @Override
        public boolean isInputCaptured() {
            return getTargetTScreen().focusedElementProperty()
                       .get() instanceof NativeTextInput || super.isInputCaptured();
        }
        //#endif
    }
}
//#endif
//#if MC <= 12110
//$$ package io.github.piscescup.fabricmc.carpetgui.gui.workspace;
//$$
//$$ import io.github.piscescup.fabricmc.carpetgui.gui.model.EditableRuleView;
//$$ import io.github.piscescup.fabricmc.carpetgui.gui.model.PersistentRuleEditor;
//$$ import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleEditResult;
//$$ import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleEditor;
//$$ import io.github.piscescup.fabricmc.carpetgui.gui.model.RulePage;
//$$ import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleSource;
//$$ import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleView;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.TElement;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.TParentElement;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.other.TLabelElement;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.other.TTextureElement;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.layout.UILayout;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.panel.TPanelElement;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.panel.menu.TContextMenuPanel;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.panel.menu.TMenuBarPanel;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.screen.TScreenPlus;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.util.TDrawContext;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.util.UITexture;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.widget.TButtonWidget;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.widget.TCheckboxWidget;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.widget.TScrollBarWidget;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.widget.TSelectWidget;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.widget.TTextFieldWidget;
//$$ import io.github.thecsdev.tcdcommons.api.util.enumerations.HorizontalAlignment;
//$$ import net.minecraft.ChatFormatting;
//$$ import net.fabricmc.loader.api.FabricLoader;
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.client.gui.screens.Screen;
//$$ import net.minecraft.network.chat.Component;
//$$ import net.minecraft.resources.ResourceLocation;
//$$
//$$ import java.util.ArrayList;
//$$ import java.util.Arrays;
//$$ import java.util.LinkedHashMap;
//$$ import java.util.LinkedHashSet;
//$$ import java.util.List;
//$$ import java.util.Locale;
//$$ import java.util.Map;
//$$ import java.util.Objects;
//$$ import java.util.Set;
//$$ import java.util.function.Consumer;
//$$
//$$ /**
//$$  * TCDCommons 3/4 implementation of the same workspace used by TCDCommons 5.
//$$  * Layout, navigation, filters and rule rows deliberately follow the modern
//$$  * workspace; only the widget API differs.
//$$  */
//$$ public final class CarpetWorkspaceScreen extends TScreenPlus {
//$$     private static final String GROUPS_ID = "carpet-gui:rule-groups";
//$$     private static final int MENU_HEIGHT = 17;
//$$     private static final int TAB_Y = 20;
//$$     private static final int TAB_HEIGHT = 18;
//$$     private static final int BODY_Y = TAB_Y + TAB_HEIGHT;
//$$     private static final int CONTROL_HEIGHT = 20;
//$$     private static final int ROW_HEIGHT = 30;
//$$     private static final int TEXT = 0xFFFFFFFF;
//$$     private static final int MUTED = 0xFF999999;
//$$     private static final int ACCENT = 0xFFFFFF55;
//$$     private static final int FOCUS = 0xFFAAFFFF;
//$$     private static final int PANEL = 0x18000000;
//$$     private static final int BACKDROP = 0x14000000;
//$$     private static final int FRAME = 0x30000000;
//$$     private static final int BORDER = 0xAA151515;
//$$
//$$     private final Screen parent;
//$$     private final RuleSource source;
//$$     private final List<? extends RulePage> sourcePages;
//$$     private final List<? extends RulePage> pages;
//$$     private final AllRulesPage allRules;
//$$     private final Set<String> openedTabs = new LinkedHashSet<>();
//$$     private final Map<TTextFieldWidget, RuleView> valueEditors = new LinkedHashMap<>();
//$$     private String selectedId;
//$$     private String selectedGroupId;
//$$     private String observedSearch = "";
//$$     private String observedGroupSearch = "";
//$$     private Component feedback = Component.empty();
//$$     private TPanelElement rulePanel;
//$$     private TPanelElement groupCandidates;
//$$     private RuleGroupStore.Group activeGroup;
//$$     private LegacyGroupMode groupMode = LegacyGroupMode.VIEW;
//$$     private boolean creatingGroup;
//$$     private TTextFieldWidget searchBox;
//$$     private TTextFieldWidget groupSearchBox;
//$$     private TLabelElement counts;
//$$     private TLabelElement notice;
//$$     private int refreshTicks;
//$$     private long observedRevision;
//$$
//$$     public CarpetWorkspaceScreen(Screen parent, RuleSource source, String initialPageId) {
//$$         super(Component.translatable("carpet-gui.screen.title"));
//$$         this.parent = parent;
//$$         this.source = Objects.requireNonNull(source);
//$$         sourcePages = List.copyOf(source.pages());
//$$         allRules = new AllRulesPage(sourcePages);
//$$         List<RulePage> navigation = new ArrayList<>();
//$$         navigation.add(allRules);
//$$         navigation.addAll(sourcePages);
//$$         pages = List.copyOf(navigation);
//$$         selectedId = GROUPS_ID.equals(initialPageId) || pages.stream().anyMatch(page -> page.id().equals(initialPageId))
//$$             ? initialPageId : null;
//$$         openedTabs.add(allRules.id());
//$$         if (selectedId != null) openedTabs.add(selectedId);
//$$         pages.forEach(page -> RuleBrowserModel.forPage(page.id()).invalidateSearch());
//$$     }
//$$
//$$     @Override
//$$     protected void init() {
//$$         valueEditors.clear();
//$$         allRules.refreshIndex();
//$$         int x = Math.max(4, getWidth() / 40);
//$$         int width = Math.max(1, getWidth() - x * 2);
//$$         int bodyHeight = Math.max(1, getHeight() - BODY_Y - 10);
//$$
//$$         TPanelElement frame = panel(x, TAB_Y - 1, width, bodyHeight + BODY_Y - TAB_Y + 1, FRAME);
//$$         addChild(frame);
//$$         initMenus(x, width);
//$$         initTabs(x, width);
//$$
//$$         if (GROUPS_ID.equals(selectedId)) initGroups(x, BODY_Y, width, bodyHeight);
//$$         else if (page() == null) initHome(x, BODY_Y, width, bodyHeight);
//$$         else initRules(page(), x, BODY_Y, width, bodyHeight);
//$$
//$$         notice = label(this, feedback, x + 4, getHeight() - 18, width - 8, 14, MUTED, HorizontalAlignment.LEFT);
//$$         observedRevision = source.revision();
//$$         source.refresh();
//$$     }
//$$
//$$     private void initMenus(int x, int width) {
//$$         TMenuBarPanel menu = new TMenuBarPanel(x, 0, width);
//$$         menu.setBackgroundColor(0x45000000);
//$$         menu.setOutlineColor(BORDER);
//$$         addChild(menu);
//$$         menu.addButton(tr("file"), file -> openMenu(file, List.of(
//$$             new LegacyMenuAction(tr("home"), () -> selectPage(null)),
//$$             new LegacyMenuAction(tr("refresh"), () -> {
//$$                 source.refresh();
//$$                 allRules.refreshIndex();
//$$                 rebuild();
//$$             })
//$$         )));
//$$         menu.addButton(tr("view"), this::openViewMenu);
//$$         menu.addButton(tr("about"), about -> openMenu(about, List.of(
//$$             new LegacyMenuAction(Component.literal("Carpet GUI " + version()), () -> selectPage(null)),
//$$             new LegacyMenuAction(tr("help"), () -> selectPage(null))
//$$         )));
//$$     }
//$$
//$$     private void openViewMenu(TElement target) {
//$$         List<LegacyMenuAction> actions = new ArrayList<>();
//$$         actions.add(new LegacyMenuAction(tr("home"), () -> selectPage(null)));
//$$         actions.add(new LegacyMenuAction(tr("rule_groups"), () -> selectPage(GROUPS_ID)));
//$$         pages.forEach(page -> actions.add(new LegacyMenuAction(page.title(), () -> selectPage(page.id()))));
//$$         openMenu(target, actions);
//$$     }
//$$
//$$     private void openMenu(TElement target, List<LegacyMenuAction> actions) {
//$$         TContextMenuPanel popup = new TContextMenuPanel(target);
//$$         for (LegacyMenuAction action : actions) {
//$$             popup.addButton(action.label(), ignored -> {
//$$                 popup.close();
//$$                 action.action().run();
//$$             });
//$$         }
//$$         popup.open();
//$$     }
//$$
//$$     private void initTabs(int x, int width) {
//$$         TPanelElement tabs = panel(x + 1, TAB_Y, width - 2, TAB_HEIGHT, 0x35000000);
//$$         addChild(tabs);
//$$         int tabX = x + 1;
//$$         int homeWidth = Math.max(64, textWidth(tr("home")) + 24);
//$$         add(tabs, button(tabX, TAB_Y, homeWidth, TAB_HEIGHT, Component.literal("    ").append(tr("home")), () -> selectPage(null), selectedId == null)
//$$             .align(HorizontalAlignment.LEFT));
//$$         add(tabs, new TTextureElement(tabX + 4, TAB_Y + 2, 14, 14, rootTexture("icon.png")));
//$$         tabX += homeWidth;
//$$         List<String> tabIds = new ArrayList<>(openedTabs);
//$$         tabIds.sort((left, right) -> Boolean.compare(!GROUPS_ID.equals(left), !GROUPS_ID.equals(right)));
//$$         for (String id : tabIds) {
//$$             Component title;
//$$             if (GROUPS_ID.equals(id)) title = tr("rule_groups");
//$$             else {
//$$                 RulePage candidate = pages.stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);
//$$                 if (candidate == null) continue;
//$$                 title = candidate.title();
//$$             }
//$$             int tabWidth = Math.min(190, Math.max(92, textWidth(title) + 39));
//$$             if (tabX + tabWidth > x + width - 2) break;
//$$             int pageWidth = tabWidth - 19;
//$$             add(tabs, button(tabX, TAB_Y, pageWidth, TAB_HEIGHT, Component.literal("    ").append(title), () -> selectPage(id), Objects.equals(selectedId, id))
//$$                 .align(HorizontalAlignment.LEFT));
//$$             add(tabs, new TTextureElement(tabX + 4, TAB_Y + 2, 14, 14, rootTexture("icon.png")));
//$$             add(tabs, button(tabX + pageWidth, TAB_Y, 19, TAB_HEIGHT, Component.literal("×"), () -> closeTab(id), false));
//$$             tabX += tabWidth;
//$$         }
//$$     }
//$$
//$$     private void initHome(int x, int y, int width, int height) {
//$$         int gap = 8;
//$$         int inset = 12;
//$$         int leftWidth = Math.max(1, (width - inset * 2 - gap) * 2 / 3);
//$$         int rightWidth = Math.max(1, width - inset * 2 - gap - leftWidth);
//$$         int leftX = x + inset;
//$$         int top = y + inset;
//$$         TPanelElement left = panel(leftX, top, leftWidth, Math.max(1, height - inset * 2), PANEL);
//$$         TPanelElement right = panel(leftX + leftWidth + gap, top, rightWidth, Math.max(1, height - inset * 2), PANEL);
//$$         addChild(left);
//$$         addChild(right);
//$$
//$$         int cardX = leftX + 12;
//$$         int cardWidth = leftWidth - 24;
//$$         TPanelElement header = panel(cardX - 4, top + 8, cardWidth + 8, 40, 0x24000000);
//$$         add(left, header);
//$$         add(header, new TTextureElement(cardX, top + 12, 30, 30, rootTexture("icon.png")));
//$$         label(header, Component.literal("Carpet GUI"), cardX + 38, top + 13, cardWidth - 42, 13, ACCENT, HorizontalAlignment.LEFT);
//$$         label(header, Component.literal(version()), cardX + 38, top + 29, cardWidth - 42, 12, MUTED, HorizontalAlignment.LEFT);
//$$         int ruleCount = sourcePages.stream().mapToInt(page -> page.rules().size()).sum();
//$$
//$$         int quickY = top + 61;
//$$         label(left, tr("quick_access"), cardX, quickY, cardWidth, 18, ACCENT, HorizontalAlignment.LEFT);
//$$         quickY += 25;
//$$         int columns = Math.max(1, cardWidth / 42);
//$$         int quickRows = Math.max(1, (sourcePages.size() + columns - 1) / columns);
//$$         int quickAccessHeight = quickRows * 42;
//$$         TPanelElement quickCard = panel(cardX - 4, quickY - 4, cardWidth + 8, quickAccessHeight + 2, 0x24000000);
//$$         add(left, quickCard);
//$$         for (int index = 0; index < sourcePages.size(); index++) {
//$$             RulePage page = sourcePages.get(index);
//$$             int tileX = cardX + index % columns * 42;
//$$             int tileY = quickY + index / columns * 42;
//$$             add(quickCard, button(tileX, tileY, 36, 36, Component.empty(), () -> selectPage(page.id()), false)
//$$                 .withTexture(rootTexture("icon.png")));
//$$         }
//$$
//$$         int featuresY = quickY + quickAccessHeight + 16;
//$$         label(left, tr("features"), cardX, featuresY, cardWidth, 18, ACCENT, HorizontalAlignment.LEFT);
//$$         TPanelElement featureCard = panel(cardX - 4, featuresY + 18, cardWidth + 8, 46, 0x24000000);
//$$         add(left, featureCard);
//$$         add(featureCard, new TTextureElement(cardX + 2, featuresY + 25, 27, 27, rootTexture("icon.png")));
//$$         add(featureCard, button(cardX + 36, featuresY + 22, cardWidth - 40, 18, tr("rule_groups"),
//$$             () -> selectPage(GROUPS_ID), false).align(HorizontalAlignment.LEFT));
//$$         label(featureCard, tr("rule_groups_home"), cardX + 36, featuresY + 40, cardWidth - 42, 14,
//$$             MUTED, HorizontalAlignment.LEFT);
//$$         if (featuresY + 82 < top + height - 12) {
//$$             label(left, tr("news"), cardX, featuresY + 78, cardWidth, 18, ACCENT, HorizontalAlignment.LEFT);
//$$         }
//$$
//$$         int rightX = right.getX() + 12;
//$$         int rightInnerWidth = rightWidth - 24;
//$$         label(right, tr("overview"), rightX, top + 10, rightInnerWidth, 18, ACCENT, HorizontalAlignment.LEFT);
//$$         label(right, Component.translatable("carpet-gui.workspace.overview_counts",
//$$             sourcePages.stream().filter(candidate -> !candidate.isVanilla()).count(), ruleCount),
//$$             rightX, top + 35, rightInnerWidth, 30, TEXT, HorizontalAlignment.LEFT);
//$$         label(right, tr(Minecraft.getInstance().getConnection() == null ? "offline" : "connected"),
//$$             rightX, top + 78, rightInnerWidth, 32, MUTED, HorizontalAlignment.LEFT);
//$$         label(right, tr("help"), rightX, top + 124, rightInnerWidth, 18, ACCENT, HorizontalAlignment.LEFT);
//$$         TPanelElement help = panel(rightX, top + 150, rightInnerWidth, Math.max(24, height - 224), 0);
//$$         help.setOutlineColor(0);
//$$         add(right, help);
//$$         UILayout.initWrappedLines(help, tr("help_body"), MUTED);
//$$     }
//$$
//$$     private void initRules(RulePage page, int x, int y, int width, int height) {
//$$         RuleBrowserModel model = RuleBrowserModel.forPage(page.id());
//$$         int sidebarWidth = Math.clamp(width * 30 / 100, Math.min(120, width / 3), 235);
//$$         TPanelElement sidebar = panel(x, y, sidebarWidth, height, PANEL);
//$$         addChild(sidebar);
//$$
//$$         int sx = x + 9;
//$$         int sw = Math.max(1, sidebarWidth - 18);
//$$         int sy = y + 9;
//$$         label(sidebar, tr("filters"), sx, sy, sw, 18, TEXT, HorizontalAlignment.CENTER);
//$$         sy += 23;
//$$
//$$         List<LegacyOption<String>> categories = new ArrayList<>();
//$$         categories.add(new LegacyOption<>("", tr("all_categories")));
//$$         model.categories(page).forEach(category ->
//$$             categories.add(new LegacyOption<>(category, categoryLabel(page, category))));
//$$         categories.add(new LegacyOption<>(RuleBrowserModel.FAVORITES, tr("favorites")));
//$$         add(sidebar, dropdown(sx, sy, sw, model.category, categories, category -> {
//$$             model.category = category;
//$$             rebuild();
//$$         }));
//$$         sy += 25;
//$$
//$$         searchBox = new TTextFieldWidget(sx, sy, sw, CONTROL_HEIGHT);
//$$         searchBox.setPlaceholderText(tr("search"));
//$$         searchBox.setInput(model.query, false);
//$$         observedSearch = model.query;
//$$         add(sidebar, searchBox);
//$$         sy += 25;
//$$
//$$         addFilter(sidebar, sx, sy, sw, tr("modified_only"), model.modifiedOnly, checked -> {
//$$             model.modifiedOnly = checked;
//$$             rebuildRuleRows();
//$$         });
//$$         sy += 27;
//$$         addFilter(sidebar, sx, sy, sw, tr("initial_difference_only"), model.initialDifferenceOnly, checked -> {
//$$             model.initialDifferenceOnly = checked;
//$$             rebuildRuleRows();
//$$         });
//$$         sy += 27;
//$$         addFilter(sidebar, sx, sy, sw, tr("saved_default_only"), model.savedDefaultOnly, checked -> {
//$$             model.savedDefaultOnly = checked;
//$$             rebuildRuleRows();
//$$         });
//$$         sy += 29;
//$$
//$$         addIconDropdown(sidebar, sx, sy, sw, "filter_sort", model.sort,
//$$             enumOptions("sort", RuleBrowserModel.Sort.values()), value -> {
//$$             model.sort = value;
//$$             rebuild();
//$$         });
//$$         sy += 25;
//$$         addIconDropdown(sidebar, sx, sy, sw, "filter_group", model.grouping,
//$$             enumOptions("grouping", RuleBrowserModel.Grouping.values()), value -> {
//$$             model.grouping = value;
//$$             rebuild();
//$$         });
//$$         sy += 25;
//$$         addIconDropdown(sidebar, sx, sy, sw, "filter_unit_dist", model.distance,
//$$             enumOptions("distance", RuleBrowserModel.Distance.values()), value -> {
//$$             model.distance = value;
//$$             rebuild();
//$$         });
//$$         sy += 25;
//$$         addIconDropdown(sidebar, sx, sy, sw, "filter_unit_time", model.time,
//$$             enumOptions("time", RuleBrowserModel.Time.values()), value -> {
//$$             model.time = value;
//$$             rebuild();
//$$         });
//$$         sy += 27;
//$$
//$$         int half = Math.max(1, (sw - 4) / 2);
//$$         add(sidebar, button(sx, sy, half, CONTROL_HEIGHT, Component.translatable("carpet-gui.expand_all"), () -> {
//$$             model.expandAll(page, true);
//$$             rebuildRuleRows();
//$$         }, false));
//$$         add(sidebar, button(sx + half + 4, sy, sw - half - 4, CONTROL_HEIGHT,
//$$             Component.translatable("carpet-gui.collapse_all"), () -> {
//$$                 model.expandAll(page, false);
//$$                 rebuildRuleRows();
//$$             }, false));
//$$
//$$         int listX = x + sidebarWidth;
//$$         int listWidth = Math.max(1, width - sidebarWidth);
//$$         counts = label(this, Component.empty(), listX + 10, y + 7, listWidth - 20, 18, MUTED, HorizontalAlignment.LEFT);
//$$         rulePanel = panel(listX + 2, y + 23, Math.max(1, listWidth - 13), Math.max(1, height - 23), PANEL);
//$$         rulePanel.setScrollFlags(TPanelElement.SCROLL_VERTICAL);
//$$         rulePanel.setSmoothScroll(true);
//$$         rulePanel.setScrollPadding(6);
//$$         addChild(rulePanel);
//$$         addChild(new TScrollBarWidget(listX + listWidth - 10, y + 23, 8, Math.max(1, height - 23), rulePanel));
//$$         rebuildRuleRows();
//$$     }
//$$
//$$     private void initGroups(int x, int y, int width, int height) {
//$$         int actionsHeight = 34;
//$$         int contentHeight = Math.max(1, height - actionsHeight);
//$$         int leftWidth = Math.clamp(width * 18 / 100, Math.min(100, width / 3), 175);
//$$         int rightWidth = Math.clamp(width * 24 / 100, Math.min(130, width / 3), 250);
//$$         int centerWidth = Math.max(1, width - leftWidth - rightWidth);
//$$         TPanelElement left = panel(x, y, leftWidth, height, PANEL);
//$$         TPanelElement center = panel(x + leftWidth, y, centerWidth, contentHeight, PANEL);
//$$         TPanelElement right = panel(x + leftWidth + centerWidth, y, rightWidth, height, PANEL);
//$$         center.setScrollFlags(TPanelElement.SCROLL_VERTICAL);
//$$         center.setSmoothScroll(true);
//$$         center.setScrollPadding(6);
//$$         addChild(left);
//$$         addChild(center);
//$$         addChild(right);
//$$
//$$         label(left, tr("rule_groups"), x + 9, y + 9, leftWidth - 18, 18, ACCENT, HorizontalAlignment.LEFT);
//$$         add(left, button(x + 9, y + 34, leftWidth - 18, CONTROL_HEIGHT, tr(creatingGroup ? "cancel" : "new_group"), () -> {
//$$             creatingGroup = !creatingGroup;
//$$             rebuild();
//$$         }, false));
//$$
//$$         int gy = y + 61;
//$$         if (creatingGroup) {
//$$             TTextFieldWidget groupName = new TTextFieldWidget(x + 9, gy, leftWidth - 18, CONTROL_HEIGHT);
//$$             groupName.setPlaceholderText(tr("group_name"));
//$$             add(left, groupName);
//$$             gy += 24;
//$$             TTextFieldWidget groupTag = new TTextFieldWidget(x + 9, gy, leftWidth - 18, CONTROL_HEIGHT);
//$$             groupTag.setPlaceholderText(tr("group_tag"));
//$$             add(left, groupTag);
//$$             gy += 24;
//$$             add(left, button(x + 9, gy, leftWidth - 18, CONTROL_HEIGHT, tr("create_group"), () -> {
//$$                 String name = groupName.getInput().strip();
//$$                 if (name.isEmpty()) {
//$$                     showFeedback(tr("group_name_required"));
//$$                     return;
//$$                 }
//$$                 RuleGroupStore.Group created = RuleGroupStore.create(name, groupTag.getInput());
//$$                 selectedGroupId = created.id();
//$$                 groupMode = LegacyGroupMode.MANAGE;
//$$                 creatingGroup = false;
//$$                 showFeedback(Component.translatable("carpet-gui.workspace.group_created", name));
//$$                 rebuild();
//$$             }, false));
//$$             gy += 29;
//$$         }
//$$         List<RuleGroupStore.Group> groups = RuleGroupStore.groups();
//$$         if (selectedGroupId == null && !groups.isEmpty()) selectedGroupId = groups.getFirst().id();
//$$         for (RuleGroupStore.Group group : groups) {
//$$             if (gy + ROW_HEIGHT > y + height - 8) break;
//$$             boolean selectedGroup = Objects.equals(selectedGroupId, group.id());
//$$             add(left, button(x + 9, gy, leftWidth - 18, ROW_HEIGHT, Component.empty(), () -> {
//$$                     selectedGroupId = group.id();
//$$                     groupMode = LegacyGroupMode.VIEW;
//$$                     rebuild();
//$$                 }, selectedGroup));
//$$             label(left, Component.literal(group.name().isBlank() ? group.id() : group.name()),
//$$                 x + 16, gy + 3, leftWidth - 32, 12, selectedGroup ? ACCENT : TEXT, HorizontalAlignment.LEFT);
//$$             String detail = (group.tag().isBlank() ? "" : group.tag() + " · ") + "r" + group.revision()
//$$                 + (group.commitsAhead() > 0 ? " ↑" + group.commitsAhead() : " ✓");
//$$             label(left, Component.literal(detail), x + 16, gy + 17, leftWidth - 32, 10, MUTED, HorizontalAlignment.LEFT);
//$$             gy += ROW_HEIGHT + 3;
//$$         }
//$$         if (groups.isEmpty()) label(left, tr("no_groups"), x + 10, gy + 5, leftWidth - 20, 35, MUTED, HorizontalAlignment.LEFT);
//$$
//$$         RuleGroupStore.Group selected = selectedGroupId == null ? null : RuleGroupStore.find(selectedGroupId);
//$$         activeGroup = selected;
//$$         int cx = x + leftWidth + 10;
//$$         int cw = Math.max(1, centerWidth - 20);
//$$         if (selected == null) {
//$$             label(center, tr("no_group_selected"), cx, y + 12, cw, 20, MUTED, HorizontalAlignment.CENTER);
//$$             initGroupHistory(right, null, x + leftWidth + centerWidth, y, rightWidth);
//$$             return;
//$$         }
//$$
//$$         switch (groupMode) {
//$$             case MANAGE -> initGroupManager(center, selected, cx, y, cw, contentHeight);
//$$             case STATUS -> initGroupStatus(center, selected, cx, y, cw);
//$$             case COMMIT -> initGroupCommit(center, selected, cx, y, cw);
//$$             default -> initGroupRuleView(center, selected, cx, y, cw);
//$$         }
//$$         initGroupHistory(right, selected, x + leftWidth + centerWidth, y, rightWidth);
//$$
//$$         TPanelElement actions = panel(x + leftWidth, y + contentHeight, centerWidth, actionsHeight - 4, 0x55000000);
//$$         addChild(actions);
//$$         int actionGap = 4;
//$$         int actionX = x + leftWidth + 7;
//$$         int actionWidth = Math.max(1, (centerWidth - 14 - actionGap * 3) / 4);
//$$         add(actions, button(actionX, y + contentHeight + 5, actionWidth, CONTROL_HEIGHT, tr("status"), () -> {
//$$             groupMode = LegacyGroupMode.STATUS;
//$$             rebuild();
//$$         }, groupMode == LegacyGroupMode.STATUS));
//$$         LegacyButton stage = button(actionX + actionWidth + actionGap, y + contentHeight + 5, actionWidth,
//$$             CONTROL_HEIGHT, tr("add"), () -> {
//$$                 int staged = RuleGroupStore.stageAll(selected, groupValues(selected));
//$$                 showFeedback(Component.translatable("carpet-gui.workspace.add_result", staged));
//$$                 groupMode = LegacyGroupMode.STATUS;
//$$                 rebuild();
//$$             }, false);
//$$         stage.setEnabled(!selected.members().isEmpty());
//$$         add(actions, stage);
//$$         LegacyButton commit = button(actionX + (actionWidth + actionGap) * 2, y + contentHeight + 5, actionWidth,
//$$             CONTROL_HEIGHT, tr("commit"), () -> {
//$$                 groupMode = LegacyGroupMode.COMMIT;
//$$                 rebuild();
//$$             }, groupMode == LegacyGroupMode.COMMIT);
//$$         commit.setEnabled(!selected.staged().isEmpty());
//$$         add(actions, commit);
//$$         LegacyButton push = button(actionX + (actionWidth + actionGap) * 3, y + contentHeight + 5,
//$$             actionWidth,
//$$             CONTROL_HEIGHT, tr("push"), () -> {
//$$                 RuleGroupStore.markPushed(selected);
//$$                 rebuild();
//$$             }, false);
//$$         push.setEnabled(selected.commitsAhead() > 0);
//$$         add(actions, push);
//$$     }
//$$
//$$     private void initGroupRuleView(TPanelElement center, RuleGroupStore.Group group, int x, int y, int width) {
//$$         label(center, Component.literal(group.name()), x, y + 8, Math.max(1, width - 105), 18, ACCENT, HorizontalAlignment.LEFT);
//$$         add(center, button(x + width - 96, y + 5, 96, CONTROL_HEIGHT, tr("manage_rules"), () -> {
//$$             groupMode = LegacyGroupMode.MANAGE;
//$$             rebuild();
//$$         }, false));
//$$         int lineY = y + 36;
//$$         label(center, Component.literal("● " + tr("color_new").getString()), x, lineY, 65, 14, 0xFFFF5555, HorizontalAlignment.LEFT);
//$$         label(center, Component.literal("● " + tr("color_modified").getString()), x + 70, lineY, 85, 14, 0xFF55AAFF, HorizontalAlignment.LEFT);
//$$         label(center, Component.literal("● " + tr("color_staged").getString()), x + 160, lineY, 70, 14, 0xFF55FF55, HorizontalAlignment.LEFT);
//$$         label(center, Component.literal("● " + tr("color_remote").getString()), x + 235, lineY,
//$$             Math.max(1, width - 315), 14, 0xFFFFAA00, HorizontalAlignment.LEFT);
//$$         label(center, Component.literal("● " + tr("color_committed").getString()), x + Math.max(315, width - 90),
//$$             lineY, 90, 14, TEXT, HorizontalAlignment.LEFT);
//$$         int rowY = lineY + 24;
//$$         if (group.members().isEmpty()) {
//$$             label(center, tr("empty_group"), x, rowY + 8, width, 35, MUTED, HorizontalAlignment.LEFT);
//$$             add(center, button(x, rowY + 48, Math.min(150, width), CONTROL_HEIGHT, tr("add_group_rules"), () -> {
//$$                 groupMode = LegacyGroupMode.MANAGE;
//$$                 rebuild();
//$$             }, false));
//$$             return;
//$$         }
//$$         for (String stateId : group.members()) {
//$$             LegacyRuleRef ref = ruleRef(stateId);
//$$             if (ref == null) {
//$$                 label(center, Component.literal(stateId), x + 5, rowY + 5, width - 10, 18, MUTED, HorizontalAlignment.LEFT);
//$$                 rowY += ROW_HEIGHT + 3;
//$$                 continue;
//$$             }
//$$             addRuleRow(center, ref.page(), ref.rule(), x, rowY, width);
//$$             rowY += ROW_HEIGHT + 3;
//$$         }
//$$     }
//$$
//$$     private void initGroupManager(TPanelElement center, RuleGroupStore.Group group, int x, int y, int width, int height) {
//$$         int doneWidth = 64;
//$$         int searchWidth = Math.clamp(width / 3, 70, 150);
//$$         int searchX = x + width - doneWidth - 6 - searchWidth;
//$$         label(center, tr("manage_group_rules"), x, y + 9, Math.max(1, searchX - x - 5), 18, ACCENT, HorizontalAlignment.LEFT);
//$$         groupSearchBox = new TTextFieldWidget(searchX, y + 5, searchWidth, CONTROL_HEIGHT);
//$$         groupSearchBox.setPlaceholderText(tr("search"));
//$$         groupSearchBox.setInput(observedGroupSearch, false);
//$$         add(center, groupSearchBox);
//$$         add(center, button(x + width - doneWidth, y + 5, doneWidth, CONTROL_HEIGHT, tr("done"), () -> {
//$$             groupMode = LegacyGroupMode.VIEW;
//$$             rebuild();
//$$         }, false));
//$$         groupCandidates = panel(x, y + 33, Math.max(1, width - 10), Math.max(1, height - 42), PANEL);
//$$         groupCandidates.setScrollFlags(TPanelElement.SCROLL_VERTICAL);
//$$         groupCandidates.setSmoothScroll(true);
//$$         add(center, groupCandidates);
//$$         add(center, new TScrollBarWidget(x + width - 8, y + 33, 8, Math.max(1, height - 42), groupCandidates));
//$$         rebuildGroupCandidates();
//$$     }
//$$
//$$     private void initGroupStatus(TPanelElement center, RuleGroupStore.Group group, int x, int y, int width) {
//$$         label(center, tr("status_title"), x, y + 9, Math.max(1, width - 70), 18, ACCENT, HorizontalAlignment.LEFT);
//$$         add(center, button(x + width - 64, y + 5, 64, CONTROL_HEIGHT, tr("back"), () -> {
//$$             groupMode = LegacyGroupMode.VIEW;
//$$             rebuild();
//$$         }, false));
//$$         int rowY = y + 38;
//$$         List<LegacyRuleRef> changed = new ArrayList<>();
//$$         for (String stateId : group.members()) {
//$$             LegacyRuleRef ref = ruleRef(stateId);
//$$             if (ref == null) continue;
//$$             String baseline = group.isStaged(stateId) ? group.stagedValue(stateId) : group.headValue(stateId);
//$$             if (!Objects.equals(baseline, ref.rule().value())) changed.add(ref);
//$$         }
//$$         label(center, Component.translatable("carpet-gui.workspace.unstaged_changes", changed.size()),
//$$             x, rowY, width, 15, ACCENT, HorizontalAlignment.LEFT);
//$$         rowY += 20;
//$$         if (changed.isEmpty()) {
//$$             label(center, tr("no_unstaged_changes"), x + 8, rowY, width - 8, 18, MUTED, HorizontalAlignment.LEFT);
//$$             rowY += 25;
//$$         }
//$$         for (LegacyRuleRef ref : changed) {
//$$             boolean fresh = !group.tracked(ref.rule().stateId());
//$$             addGroupChangeRow(center, x, rowY, width, ref.rule().label(), ref.rule().value(),
//$$                 fresh ? "?" : "M", fresh ? 0xFFFF5555 : 0xFF55AAFF, tr("stage"), () -> {
//$$                     RuleGroupStore.stage(group, ref.rule().stateId(), ref.rule().value());
//$$                     rebuild();
//$$                 });
//$$             rowY += 29;
//$$         }
//$$         rowY += 5;
//$$         label(center, Component.translatable("carpet-gui.workspace.staged_changes", group.staged().size()),
//$$             x, rowY, width, 15, ACCENT, HorizontalAlignment.LEFT);
//$$         rowY += 20;
//$$         if (group.staged().isEmpty()) {
//$$             label(center, tr("no_staged_changes"), x + 8, rowY, width - 8, 18, MUTED, HorizontalAlignment.LEFT);
//$$         }
//$$         for (Map.Entry<String, String> staged : group.staged().entrySet()) {
//$$             LegacyRuleRef ref = ruleRef(staged.getKey());
//$$             Component title = ref == null ? Component.literal(staged.getKey()) : ref.rule().label();
//$$             addGroupChangeRow(center, x, rowY, width, title, staged.getValue(), "A", 0xFF55FF55,
//$$                 tr("unstage"), () -> {
//$$                     RuleGroupStore.unstage(group, staged.getKey());
//$$                     rebuild();
//$$                 });
//$$             rowY += 29;
//$$         }
//$$     }
//$$
//$$     private void initGroupCommit(TPanelElement center, RuleGroupStore.Group group, int x, int y, int width) {
//$$         label(center, tr("create_commit"), x, y + 9, Math.max(1, width - 70), 18, ACCENT, HorizontalAlignment.LEFT);
//$$         add(center, button(x + width - 64, y + 5, 64, CONTROL_HEIGHT, tr("back"), () -> {
//$$             groupMode = LegacyGroupMode.STATUS;
//$$             rebuild();
//$$         }, false));
//$$         label(center, tr("commit_message"), x, y + 39, width, 14, TEXT, HorizontalAlignment.LEFT);
//$$         TTextFieldWidget message = new TTextFieldWidget(x, y + 56, width, CONTROL_HEIGHT);
//$$         message.setPlaceholderText(tr("commit_message_hint"));
//$$         add(center, message);
//$$         LegacyButton commit = button(x, y + 83, Math.min(180, width), CONTROL_HEIGHT,
//$$             Component.translatable("carpet-gui.workspace.commit_count", group.staged().size()), () -> {
//$$                 if (message.getInput().isBlank()) {
//$$                     showFeedback(tr("commit_message_required"));
//$$                     return;
//$$                 }
//$$                 if (RuleGroupStore.commit(group, message.getInput())) {
//$$                     groupMode = LegacyGroupMode.VIEW;
//$$                     rebuild();
//$$                 }
//$$             }, false);
//$$         commit.setEnabled(!group.staged().isEmpty());
//$$         add(center, commit);
//$$         int rowY = y + 118;
//$$         label(center, Component.translatable("carpet-gui.workspace.staged_changes", group.staged().size()),
//$$             x, rowY, width, 15, ACCENT, HorizontalAlignment.LEFT);
//$$         rowY += 20;
//$$         for (Map.Entry<String, String> staged : group.staged().entrySet()) {
//$$             LegacyRuleRef ref = ruleRef(staged.getKey());
//$$             Component title = ref == null ? Component.literal(staged.getKey()) : ref.rule().label();
//$$             addGroupChangeRow(center, x, rowY, width, title, staged.getValue(), "A", 0xFF55FF55, null, null);
//$$             rowY += 29;
//$$         }
//$$         if (group.staged().isEmpty()) {
//$$             label(center, tr("nothing_to_commit"), x + 8, rowY, width - 8, 18, MUTED, HorizontalAlignment.LEFT);
//$$         }
//$$     }
//$$
//$$     private void addGroupChangeRow(TPanelElement parent, int x, int y, int width, Component title,
//$$                                    String detail, String marker, int color, Component actionText, Runnable action) {
//$$         TPanelElement row = panel(x, y, width, 26, 0x28000000);
//$$         add(parent, row);
//$$         int actionWidth = action == null ? 0 : Math.clamp(width / 5, 46, 70);
//$$         label(row, Component.literal(marker), x + 6, y + 7, 12, 11, color, HorizontalAlignment.LEFT);
//$$         label(row, title, x + 21, y + 3, Math.max(1, width - actionWidth - 29), 11, color, HorizontalAlignment.LEFT);
//$$         label(row, Component.literal(detail), x + 21, y + 14, Math.max(1, width - actionWidth - 29), 10,
//$$             MUTED, HorizontalAlignment.LEFT);
//$$         if (action != null) {
//$$             add(row, button(x + width - actionWidth - 3, y + 3, actionWidth, 20, actionText, action, false));
//$$         }
//$$     }
//$$
//$$     private void initGroupHistory(TPanelElement right, RuleGroupStore.Group group, int x, int y, int width) {
//$$         int rx = x + 10;
//$$         int rw = Math.max(1, width - 20);
//$$         label(right, tr("process"), rx, y + 9, rw, 18, ACCENT, HorizontalAlignment.LEFT);
//$$         if (group == null) {
//$$             label(right, tr("no_group_selected"), rx, y + 38, rw, 35, MUTED, HorizontalAlignment.LEFT);
//$$             return;
//$$         }
//$$         label(right, Component.literal(group.name()), rx, y + 39, rw, 18, TEXT, HorizontalAlignment.LEFT);
//$$         label(right, Component.translatable("carpet-gui.workspace.pipeline_summary", group.members().size(),
//$$             group.staged().size(), group.commitsAhead()), rx, y + 65, rw, 35, MUTED, HorizontalAlignment.LEFT);
//$$         int historyY = y + 112;
//$$         if (group.history().isEmpty()) {
//$$             label(right, tr("no_commits_graph"), rx, historyY, rw, 55, MUTED, HorizontalAlignment.LEFT);
//$$             return;
//$$         }
//$$         for (RuleGroupStore.Commit history : group.history()) {
//$$             boolean pending = history.revision() > group.pushedRevision();
//$$             List<String> messageLines = wrapLines(history.message(), Math.max(1, rw - 14));
//$$             for (int index = 0; index < messageLines.size(); index++) {
//$$                 String prefix = index == 0 ? "● " : "   ";
//$$                 label(right, Component.literal(prefix + messageLines.get(index)), rx, historyY, rw, 12,
//$$                     pending ? ACCENT : TEXT, HorizontalAlignment.LEFT);
//$$                 historyY += 12;
//$$             }
//$$             label(right, Component.literal("r" + history.revision() + (pending ? "  local" : "  server")),
//$$                 rx + 12, historyY + 2, rw - 12, 12, MUTED, HorizontalAlignment.LEFT);
//$$             historyY += 22;
//$$         }
//$$     }
//$$
//$$     private Map<String, String> groupValues(RuleGroupStore.Group group) {
//$$         Map<String, String> values = new LinkedHashMap<>();
//$$         for (String stateId : group.members()) {
//$$             LegacyRuleRef ref = ruleRef(stateId);
//$$             if (ref != null) values.put(stateId, ref.rule().value());
//$$         }
//$$         return values;
//$$     }
//$$
//$$     private LegacyRuleRef ruleRef(String stateId) {
//$$         for (RulePage page : sourcePages) {
//$$             for (RuleView rule : page.rules()) {
//$$                 if (rule.stateId().equals(stateId)) return new LegacyRuleRef(page, rule);
//$$             }
//$$         }
//$$         return null;
//$$     }
//$$
//$$     private void rebuildGroupCandidates() {
//$$         if (groupCandidates == null || activeGroup == null) return;
//$$         groupCandidates.clearChildren();
//$$         String needle = observedGroupSearch.strip().toLowerCase(Locale.ROOT);
//$$         int rx = groupCandidates.getX();
//$$         int rw = groupCandidates.getWidth();
//$$         int cy = groupCandidates.getY() + 4;
//$$         for (RulePage page : sourcePages) {
//$$             for (RuleView rule : page.rules()) {
//$$                 if (!needle.isEmpty() && rule.searchTerms().stream()
//$$                     .map(term -> term.toLowerCase(Locale.ROOT))
//$$                     .noneMatch(term -> term.contains(needle))) continue;
//$$                 boolean member = activeGroup.members().contains(rule.stateId());
//$$                 add(groupCandidates, button(rx + 4, cy, rw - 8, CONTROL_HEIGHT,
//$$                     Component.literal((member ? "− " : "+ ") + rule.label().getString()), () -> {
//$$                         if (activeGroup.members().contains(rule.stateId())) RuleGroupStore.removeMember(activeGroup, rule.stateId());
//$$                         else RuleGroupStore.addMember(activeGroup, rule.stateId());
//$$                         rebuildGroupCandidates();
//$$                     }, member));
//$$                 cy += 23;
//$$             }
//$$         }
//$$     }
//$$
//$$     private void addFilter(TParentElement parent, int x, int y, int width, Component text,
//$$                            boolean checked, Consumer<Boolean> changed) {
//$$         TCheckboxWidget checkbox = new TCheckboxWidget(x, y, width, CONTROL_HEIGHT, text, checked);
//$$         checkbox.setHorizontalAlignment(HorizontalAlignment.LEFT, HorizontalAlignment.LEFT);
//$$         checkbox.eClicked.register(ignored -> changed.accept(checkbox.getChecked()));
//$$         add(parent, checkbox);
//$$     }
//$$
//$$     private void rebuildRuleRows() {
//$$         RulePage page = page();
//$$         if (rulePanel == null || page == null) return;
//$$         RuleBrowserModel model = RuleBrowserModel.forPage(page.id());
//$$         rulePanel.clearChildren();
//$$         valueEditors.clear();
//$$         List<RuleView> filtered = model.rules(page);
//$$         int x = rulePanel.getX() + 4;
//$$         int width = Math.max(1, rulePanel.getWidth() - 8);
//$$         int y = rulePanel.getY() + 4;
//$$
//$$         List<RuleBrowserModel.Group> groups = model.groups(page);
//$$         if (counts != null) counts.setText(Component.translatable("carpet-gui.counts", filtered.size(), groups.size()));
//$$         if (groups.isEmpty()) {
//$$             label(rulePanel, tr(filtered.isEmpty() && model.query.isBlank() ? "no_rules" : "no_matching_rules"),
//$$                 x, y + 8, width, 20, MUTED, HorizontalAlignment.CENTER);
//$$             return;
//$$         }
//$$
//$$         for (RuleBrowserModel.Group group : groups) {
//$$             Component title = categoryLabel(page, group.category());
//$$             add(rulePanel, button(x, y, width, 20,
//$$                 Component.literal((model.expanded(group.category()) ? "▾ " : "▸ ") + title.getString()
//$$                     + "  (" + group.rules().size() + ")"), () -> {
//$$                     model.setExpanded(group.category(), !model.expanded(group.category()));
//$$                     rebuildRuleRows();
//$$                 }, false));
//$$             y += 23;
//$$             if (!model.expanded(group.category())) continue;
//$$             for (RuleView rule : group.rules()) {
//$$                 addRuleRow(rulePanel, page, rule, x, y, width);
//$$                 y += ROW_HEIGHT + 2;
//$$             }
//$$             y += 4;
//$$         }
//$$     }
//$$
//$$     private void addRuleRow(TPanelElement parent, RulePage page, RuleView rule, int x, int y, int width) {
//$$         TPanelElement row = panel(x, y, width, ROW_HEIGHT, 0x24000000);
//$$         add(parent, row);
//$$         int favoriteWidth = 22;
//$$         int resetWidth = 22;
//$$         int saveWidth = rule instanceof EditableRuleView editable && editable.editor() instanceof PersistentRuleEditor ? 22 : 0;
//$$         int valueWidth = Math.clamp(width / 5, 56, 92);
//$$         int favoriteX = x + width - favoriteWidth - 3;
//$$         int resetX = favoriteX - resetWidth - 3;
//$$         int saveX = resetX - saveWidth - (saveWidth == 0 ? 0 : 3);
//$$         int valueX = saveX - valueWidth - 4;
//$$         int textWidth = Math.max(1, valueX - x - 8);
//$$
//$$         label(row, rule.label(), x + 5, y + 2, textWidth, 12,
//$$             rule.differsFromConfiguredValue() ? ACCENT : TEXT, HorizontalAlignment.LEFT);
//$$         label(row, rule.description(), x + 5, y + 15, textWidth, 11, MUTED, HorizontalAlignment.LEFT);
//$$
//$$         RuleEditor editor = rule instanceof EditableRuleView editable ? editable.editor() : null;
//$$         if (editor != null && (editor.inputKind() == RuleEditor.InputKind.BOOLEAN
//$$             || editor.strict() && !editor.suggestions().isEmpty())) {
//$$             List<String> values = new ArrayList<>(new LinkedHashSet<>(editor.suggestions()));
//$$             if (editor.inputKind() == RuleEditor.InputKind.BOOLEAN && values.isEmpty()) {
//$$                 values.add("false");
//$$                 values.add("true");
//$$             }
//$$             List<LegacyOption<String>> valueOptions = values.stream()
//$$                 .map(value -> new LegacyOption<>(value, ruleValueLabel(value)))
//$$                 .toList();
//$$             TSelectWidget<TSelectWidget.SimpleEntry> value = dropdown(valueX, y + 4, valueWidth, rule.value(), valueOptions,
//$$                 selected -> submit(rule, editor, selected));
//$$             value.setEnabled(editor.editable());
//$$             add(row, value);
//$$         } else if (editor != null) {
//$$             TTextFieldWidget value = new TTextFieldWidget(valueX, y + 4, valueWidth, CONTROL_HEIGHT);
//$$             value.setInput(rule.value(), false);
//$$             value.setEnabled(editor.editable());
//$$             add(row, value);
//$$             valueEditors.put(value, rule);
//$$         } else {
//$$             LegacyButton value = button(valueX, y + 4, valueWidth, CONTROL_HEIGHT, Component.literal(rule.value()), () -> {}, false);
//$$             value.setEnabled(false);
//$$             add(row, value);
//$$         }
//$$
//$$         LegacyButton reset = iconButton(resetX, y + 4, resetWidth, CONTROL_HEIGHT, "actions/reset", () -> {
//$$             if (editor != null) submit(rule, editor, rule.defaultValue());
//$$         }, false);
//$$         reset.setEnabled(editor != null && editor.editable() && !rule.value().equals(rule.defaultValue()));
//$$         add(row, reset);
//$$         if (saveWidth > 0) {
//$$             PersistentRuleEditor persistent = (PersistentRuleEditor) editor;
//$$             add(row, iconButton(saveX, y + 4, saveWidth, CONTROL_HEIGHT,
//$$                 persistent.isSavedDefault(rule.value()) ? "actions/lock_closed" : "actions/lock_open", () -> {
//$$                 RuleEditResult result = persistent.saveDefault(rule.value(), completed -> showFeedback(completed.message()));
//$$                 feedback(rule, result);
//$$             }, false));
//$$         }
//$$         boolean favorite = FavoriteRules.contains(rule.stateId());
//$$         add(row, iconButton(favoriteX, y + 4, favoriteWidth, CONTROL_HEIGHT,
//$$             favorite ? "actions/favorite_on" : "actions/favorite_off", () -> {
//$$                 FavoriteRules.toggle(rule.stateId());
//$$                 rebuildRuleRows();
//$$             }, favorite));
//$$     }
//$$
//$$     private void submit(RuleView rule, RuleEditor editor, String value) {
//$$         if (!editor.editable()) return;
//$$         RuleEditResult result = editor.submit(value, completed -> showFeedback(completed.message()));
//$$         feedback(rule, result);
//$$         source.refresh();
//$$         rebuildRuleRows();
//$$     }
//$$
//$$     private void feedback(RuleView rule, RuleEditResult result) {
//$$         feedback = rule.label().copy().append(": ").append(result.message());
//$$         if (notice != null) notice.setText(feedback);
//$$     }
//$$
//$$     private void showFeedback(Component message) {
//$$         feedback = message;
//$$         if (notice != null) notice.setText(feedback);
//$$     }
//$$
//$$     private RulePage page() {
//$$         return pages.stream().filter(candidate -> candidate.id().equals(selectedId)).findFirst().orElse(null);
//$$     }
//$$
//$$     private void selectPage(String id) {
//$$         selectedId = id;
//$$         if (id != null) openedTabs.add(id);
//$$         feedback = Component.empty();
//$$         rebuild();
//$$     }
//$$
//$$     private void closeTab(String id) {
//$$         openedTabs.remove(id);
//$$         if (Objects.equals(selectedId, id)) selectedId = null;
//$$         feedback = Component.empty();
//$$         rebuild();
//$$     }
//$$
//$$     private void cyclePage() {
//$$         if (GROUPS_ID.equals(selectedId)) {
//$$             selectPage(null);
//$$             return;
//$$         }
//$$         RulePage current = page();
//$$         if (current == null) selectPage(pages.isEmpty() ? GROUPS_ID : pages.getFirst().id());
//$$         else {
//$$             int index = pages.indexOf(current);
//$$             selectPage(index + 1 < pages.size() ? pages.get(index + 1).id() : GROUPS_ID);
//$$         }
//$$     }
//$$
//$$     private void rebuild() {
//$$         clearChildren();
//$$         init();
//$$     }
//$$
//$$     @Override
//$$     protected void tick() {
//$$         if (searchBox != null) {
//$$             String value = searchBox.getInput();
//$$             if (!value.equals(observedSearch)) {
//$$                 observedSearch = value;
//$$                 RulePage page = page();
//$$                 if (page != null) RuleBrowserModel.forPage(page.id()).query = value;
//$$                 rebuildRuleRows();
//$$             }
//$$         }
//$$         if (groupSearchBox != null && !groupSearchBox.getInput().equals(observedGroupSearch)) {
//$$             observedGroupSearch = groupSearchBox.getInput();
//$$             rebuildGroupCandidates();
//$$         }
//$$         for (Map.Entry<TTextFieldWidget, RuleView> entry : List.copyOf(valueEditors.entrySet())) {
//$$             TTextFieldWidget field = entry.getKey();
//$$             RuleView rule = entry.getValue();
//$$             if (!field.isFocused() && !field.getInput().equals(rule.value())
//$$                 && rule instanceof EditableRuleView editable) {
//$$                 submit(rule, editable.editor(), field.getInput());
//$$                 break;
//$$             }
//$$         }
//$$         if (++refreshTicks >= 20) {
//$$             refreshTicks = 0;
//$$             source.refresh();
//$$             if (source.revision() != observedRevision && getFocusedElement() == null) {
//$$                 observedRevision = source.revision();
//$$                 allRules.refreshIndex();
//$$                 rebuildRuleRows();
//$$             }
//$$         }
//$$     }
//$$
//$$     @Override
//$$     public void renderBackground(TDrawContext graphics) {
//$$         graphics.fill(0, 0, getWidth(), getHeight(), BACKDROP);
//$$     }
//$$
//$$     @Override
//$$     public void close() {
//$$         Minecraft.getInstance().setScreen(parent);
//$$     }
//$$
//$$     @Override
//$$     public boolean shouldPause() {
//$$         return false;
//$$     }
//$$
//$$     private static <T extends TElement> T add(TParentElement parent, T child) {
//$$         parent.addChild(child, false);
//$$         return child;
//$$     }
//$$
//$$     private TPanelElement panel(int x, int y, int width, int height, int color) {
//$$         TPanelElement panel = new TPanelElement(x, y, Math.max(1, width), Math.max(1, height));
//$$         panel.setBackgroundColor(color);
//$$         panel.setOutlineColor(BORDER);
//$$         return panel;
//$$     }
//$$
//$$     private LegacyButton button(int x, int y, int width, int height, Component text,
//$$                                 Runnable action, boolean selected) {
//$$         Component label = selected ? text.copy().withStyle(ChatFormatting.YELLOW) : text;
//$$         return new LegacyButton(x, y, Math.max(1, width), Math.max(1, height), label, action, selected);
//$$     }
//$$
//$$     private LegacyButton iconButton(int x, int y, int width, int height, String icon,
//$$                                     Runnable action, boolean selected) {
//$$         return button(x, y, width, height, Component.empty(), action, selected).withIcon(icon);
//$$     }
//$$
//$$     private <T> TSelectWidget<TSelectWidget.SimpleEntry> dropdown(int x, int y, int width, T selected,
//$$                                                                    List<LegacyOption<T>> options, Consumer<T> changed) {
//$$         TSelectWidget<TSelectWidget.SimpleEntry> select = new TSelectWidget<>(
//$$             x, y, Math.max(1, width), CONTROL_HEIGHT);
//$$         Map<TSelectWidget.SimpleEntry, T> values = new LinkedHashMap<>();
//$$         TSelectWidget.SimpleEntry selectedEntry = null;
//$$         for (LegacyOption<T> option : options) {
//$$             TSelectWidget.SimpleEntry entry = new TSelectWidget.SimpleEntry(option.label());
//$$             select.addEntry(entry);
//$$             values.put(entry, option.value());
//$$             if (Objects.equals(selected, option.value())) selectedEntry = entry;
//$$         }
//$$         if (selectedEntry != null) select.setSelected(selectedEntry, false);
//$$         select.setText(selectedEntry == null
//$$             ? Component.literal(String.valueOf(selected)) : selectedEntry.getText());
//$$         select.eSelectionChanged.register((widget, entry) -> {
//$$             if (entry != null && values.containsKey(entry)) changed.accept(values.get(entry));
//$$         });
//$$         return select;
//$$     }
//$$
//$$     private <T extends Enum<T>> List<LegacyOption<T>> enumOptions(String name, T[] values) {
//$$         return Arrays.stream(values)
//$$             .map(value -> new LegacyOption<>(value,
//$$                 tr(name + "." + value.name().toLowerCase(Locale.ROOT))))
//$$             .toList();
//$$     }
//$$
//$$     private <T> void addIconDropdown(TParentElement parent, int x, int y, int width, String icon,
//$$                                      T selected, List<LegacyOption<T>> options, Consumer<T> changed) {
//$$         TTextureElement texture = new TTextureElement(x, y, CONTROL_HEIGHT, CONTROL_HEIGHT, texture(icon));
//$$         add(parent, texture);
//$$         add(parent, dropdown(x + 27, y, Math.max(1, width - 27), selected, options, changed));
//$$     }
//$$
//$$     private static UITexture texture(String path) {
//$$         return new UITexture(ResourceLocation.fromNamespaceAndPath(
//$$             "carpet-gui", "textures/gui/icons/" + path + ".png"));
//$$     }
//$$
//$$     private static UITexture rootTexture(String path) {
//$$         return new UITexture(ResourceLocation.fromNamespaceAndPath("carpet-gui", path));
//$$     }
//$$
//$$     private TLabelElement label(TParentElement parent, Component text, int x, int y, int width,
//$$                                 int height, int color, HorizontalAlignment alignment) {
//$$         TLabelElement label = new TLabelElement(x, y, Math.max(1, width), Math.max(1, height), text);
//$$         label.setTextColor(color);
//$$         label.setTextHorizontalAlignment(alignment);
//$$         add(parent, label);
//$$         return label;
//$$     }
//$$
//$$     private Component categoryLabel(RulePage page, String category) {
//$$         if (category == null || category.isEmpty()) return tr("all_categories");
//$$         if (RuleBrowserModel.ALL_RULES.equals(category)) return Component.translatable("carpet-gui.tab.all");
//$$         if (RuleBrowserModel.FAVORITES.equals(category)) return tr("favorites");
//$$         if (RuleBrowserModel.UNCATEGORIZED.equals(category)) return tr("uncategorized");
//$$         return page.categoryLabel(category);
//$$     }
//$$
//$$     private static Component ruleValueLabel(String value) {
//$$         if ("true".equalsIgnoreCase(value)) return Component.literal(value).withStyle(ChatFormatting.GREEN);
//$$         if ("false".equalsIgnoreCase(value)) return Component.literal(value).withStyle(ChatFormatting.RED);
//$$         return Component.literal(value);
//$$     }
//$$
//$$     private Component option(String name, Enum<?> value) {
//$$         return tr(name).copy().append(": ").append(tr(name + "." + value.name().toLowerCase(Locale.ROOT)));
//$$     }
//$$
//$$     private static <T> T next(T[] values, T value) {
//$$         int index = Arrays.asList(values).indexOf(value);
//$$         return values[(index + 1 + values.length) % values.length];
//$$     }
//$$
//$$     private int textWidth(Component text) {
//$$         return getTextRenderer().width(text);
//$$     }
//$$
//$$     private List<String> wrapLines(String text, int maxWidth) {
//$$         List<String> lines = new ArrayList<>();
//$$         for (String paragraph : text.split("\\R", -1)) {
//$$             if (paragraph.isEmpty()) {
//$$                 lines.add("");
//$$                 continue;
//$$             }
//$$             StringBuilder line = new StringBuilder();
//$$             for (int offset = 0; offset < paragraph.length();) {
//$$                 int codePoint = paragraph.codePointAt(offset);
//$$                 String character = new String(Character.toChars(codePoint));
//$$                 String candidate = line + character;
//$$                 if (line.length() > 0 && textWidth(Component.literal(candidate)) > maxWidth) {
//$$                     lines.add(line.toString().stripTrailing());
//$$                     line.setLength(0);
//$$                     if (!character.isBlank()) line.append(character);
//$$                 } else {
//$$                     line.append(character);
//$$                 }
//$$                 offset += Character.charCount(codePoint);
//$$             }
//$$             if (line.length() > 0) lines.add(line.toString().stripTrailing());
//$$         }
//$$         if (lines.isEmpty()) lines.add("");
//$$         return lines;
//$$     }
//$$
//$$     private static Component tr(String key) {
//$$         return Component.translatable("carpet-gui.workspace." + key);
//$$     }
//$$
//$$     private static String version() {
//$$         return FabricLoader.getInstance().getModContainer("carpet-gui")
//$$             .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("?");
//$$     }
//$$
//$$     private record LegacyMenuAction(Component label, Runnable action) {
//$$     }
//$$
//$$     private record LegacyOption<T>(T value, Component label) {
//$$     }
//$$
//$$     private record LegacyRuleRef(RulePage page, RuleView rule) {
//$$     }
//$$
//$$     private enum LegacyGroupMode {
//$$         VIEW,
//$$         MANAGE,
//$$         STATUS,
//$$         COMMIT
//$$     }
//$$
//$$     private static class LegacyButton extends TButtonWidget {
//$$         private final boolean selected;
//$$         private HorizontalAlignment alignment = HorizontalAlignment.CENTER;
//$$
//$$         LegacyButton(int x, int y, int width, int height, Component text, Runnable action, boolean selected) {
//$$             super(x, y, width, height, text, ignored -> action.run());
//$$             this.selected = selected;
//$$         }
//$$
//$$         LegacyButton withIcon(String icon) {
//$$             setIcon(texture(icon));
//$$             return this;
//$$         }
//$$
//$$         LegacyButton withTexture(UITexture icon) {
//$$             setIcon(icon);
//$$             return this;
//$$         }
//$$
//$$         LegacyButton align(HorizontalAlignment alignment) {
//$$             this.alignment = alignment;
//$$             return this;
//$$         }
//$$
//$$         @Override
//$$         public void render(TDrawContext graphics) {
//$$             int fill = selected ? 0x70505018 : isFocusedOrHovered() ? 0x60393939 : 0x30000000;
//$$             graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), fill);
//$$             int border = isFocusedOrHovered() ? FOCUS : BORDER;
//$$             graphics.fill(getX(), getY(), getX() + getWidth(), getY() + 1, border);
//$$             graphics.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), border);
//$$             graphics.fill(getX(), getY(), getX() + 1, getY() + getHeight(), border);
//$$             graphics.fill(getX() + getWidth() - 1, getY(), getX() + getWidth(), getY() + getHeight(), border);
//$$             if (getIcon() != null) renderIcon(graphics);
//$$             else graphics.drawTElementTextTHC(getText(), alignment, getEnabled() ? TEXT : MUTED);
//$$         }
//$$     }
//$$
//$$ }
//#endif

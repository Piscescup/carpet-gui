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
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.PreeditEvent;
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

import static org.lwjgl.sdl.SDLMouse.SDL_BUTTON_LEFT;

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
        var allRules = new AllRulesPage(sourcePages);
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
        var modified = new TCheckboxWidget(model.modifiedOnly);
        modified.setBounds(x, y, WorkspaceStyle.CONTROL_HEIGHT, WorkspaceStyle.CONTROL_HEIGHT);
        modified.checkedProperty().addChangeListener((property, previous, checked) -> {
            model.modifiedOnly = checked;
            requestListRefresh(true);
        });
        modified.tooltipProperty().set(ignored -> TTooltip.of(tr("modified_only")), CarpetWorkspaceScreen.class);
        sidebar.add(modified);
        WorkspaceStyle.label(sidebar, tr("modified_only"), x + 27, y + 6, width - 27, 12, WorkspaceStyle.TEXT);
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
        var unitsHint = WorkspaceStyle.label(sidebar, tr("units_hint"), x, y, width, 33, WorkspaceStyle.MUTED);
        unitsHint.textScaleProperty()
            .set(WorkspaceStyle.SMALL_TEXT_SCALE, CarpetWorkspaceScreen.class);
        y += 38;
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
        nextY = addParagraph(left, tr("news_intro"), contentX, nextY + 22, contentWidth, WorkspaceStyle.MUTED) + 16;
        nextY = addParagraph(left, tr("news_workspace"), contentX, nextY, contentWidth, WorkspaceStyle.TEXT) + 16;
        addParagraph(left, tr("news_addons"), contentX, nextY, contentWidth, WorkspaceStyle.TEXT);
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
        if (++refreshTicks >= 20) {
            refreshTicks = 0;
            source.refresh();
        }
        RulePage page = page();
        if (ruleList != null) {
            // Live refresh is independent of the backend's observer support.
            ruleList.refreshRows();
            boolean overlay = findChild(element -> element instanceof TContextMenu, false).isPresent();
            boolean dragging = findChild(
                element -> element instanceof TClickableWidget clickable && clickable.pressedProperty()
                    .getZ(), true
            ).isPresent();
            boolean textFocused = focusedElementProperty().get() instanceof NativeTextInput;
            if (page != null && !observedCategories.equals(RuleBrowserModel.forPage(page.id()).categories(page))
                && !ruleList.interacting() && !textFocused && !overlay && !dragging) {
                rebuildWorkspace();
                return;
            }
            if ((listDirty || observedRevision != source.revision() || ruleList.changed()) && !ruleList.interacting() && !overlay && !dragging) {
                ruleList.rebuild(resetScroll);
                listDirty = false;
                resetScroll = false;
                observedRevision = source.revision();
            }
            if (counts != null && page != null) {
                RuleBrowserModel model = RuleBrowserModel.forPage(page.id());
                counts.setText(Component.translatable(
                    "carpet-gui.counts",
                    model.rules(page)
                        .size(),
                    model.groups(page)
                        .size()
                ));
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
            context.getMouseButton() == SDL_BUTTON_LEFT && ruleList != null &&
            findChild(element -> element instanceof TContextMenu, false).isEmpty()) {
            ruleList.beforeMousePress(context.getMouseX(), context.getMouseY());
        }
        if (phase == TInputContext.InputDiscoveryPhase.BROADCAST && context.getInputType() == TInputContext.InputType.MOUSE_PRESS &&
            context.getMouseButton() == SDL_BUTTON_LEFT && RuleGroupWorkspace.ID.equals(selectedId) &&
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
    }
}

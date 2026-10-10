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
import com.thecsdev.commonmc.api.client.gui.panel.TPanelElement;
import com.thecsdev.commonmc.api.client.gui.screen.ILastScreenProvider;
import com.thecsdev.commonmc.api.client.gui.screen.TScreenPlus;
import com.thecsdev.commonmc.api.client.gui.screen.TScreenWrapper;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleEditResult;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RulePage;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleSource;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleView;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.NativeTextInput;
import net.minecraft.client.gui.screens.Screen;
//#if MC >= 260300
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.PreeditEvent;
//#endif
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * TCDCommons-powered Home/mod workspace. Editing and networking remain backend capabilities.
 */
public final class CarpetWorkspaceScreen
    extends TScreenPlus
    implements ILastScreenProvider
{
    private final Screen parent;
    private final CarpetWorkspaceEditor editor;
    private CarpetWorkspaceGUI workspace;

    public CarpetWorkspaceScreen(Screen parent, RuleSource source, String initialPageId) {
        this(parent, new CarpetWorkspaceEditor(source, initialPageId));
    }

    public CarpetWorkspaceScreen(Screen parent, CarpetWorkspaceEditor editor) {
        super(Component.translatable("carpet-gui.screen.title"));
        this.parent = parent;
        this.editor = Objects.requireNonNull(editor, "editor");
    }

    public CarpetWorkspaceEditor editor() {
        return editor;
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
        editor.refresh();
    }

    @Override
    protected void closeCallback() {
        if (workspace != null) workspace.closeResources();
    }

    @Override
    public void close() {
        if (workspace != null) workspace.cancelDrafts();
        focusedElementProperty().set(null, CarpetWorkspaceScreen.class);
        super.close();
    }

    @Override
    protected void initCallback() {
        if (workspace != null) workspace.cancelDrafts();
        workspace = new CarpetWorkspaceGUI(this, editor);
        workspace.setBounds(getBounds());
        add(workspace);
    }

    Component categoryLabel(RulePage page, String category) {
        return workspace.categoryLabel(page, category);
    }

    void rebuildWorkspace() {
        if (workspace != null) workspace.rebuildWorkspace();
    }

    void requestListRefresh() {
        if (workspace != null) workspace.requestListRefresh();
    }

    void feedback(RuleView rule, RuleEditResult result) {
        if (workspace != null) workspace.feedback(rule, result);
    }

    void showFeedback(Component message) {
        if (workspace != null) workspace.showFeedback(message);
    }

    void refreshRules() {
        editor.refresh();
    }

    int textWidth(String text) {
        return workspace != null
            ? workspace.textWidth(text)
            : (int) Math.ceil(getClient().font.width(text) * WorkspaceStyle.TEXT_SCALE);
    }

    void addWorkspacePane(TPanelElement panel, int x, int y, int width, int height) {
        workspace.addWorkspacePane(panel, x, y, width, height);
    }

    void addWorkspaceElement(TElement element) {
        workspace.addWorkspaceElement(element);
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
//$$ import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleBrowserModel;
//$$ import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleSource;
//$$ import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleView;
//$$ import io.github.piscescup.fabricmc.carpetgui.store.FavoriteRules;
//$$ import io.github.piscescup.fabricmc.carpetgui.store.RuleGroupStore;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.TElement;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.TParentElement;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.other.TLabelElement;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.other.TTextureElement;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.panel.TPanelElement;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.panel.menu.TContextMenuPanel;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.panel.menu.TMenuBarPanel;
//$$ import io.github.thecsdev.tcdcommons.api.client.gui.panel.menu.item.TMenuPanelButton;
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
//$$ public final class CarpetWorkspaceScreen extends TScreenPlus {
//$$     private static final String GROUPS_ID = CarpetWorkspaceEditor.RULE_GROUPS_PAGE_ID;
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
//$$     private static final int POPUP_BACKGROUND = 0xFF383838;
//$$     private static final int POPUP_BORDER = 0xFF707070;
//$$     private static final int POPUP_HOVER = 0xFF505050;
//$$
//$$     private final Screen parent;
//$$     private final CarpetWorkspaceEditor editor;
//$$     private final Map<TTextFieldWidget, RuleView> valueEditors = new LinkedHashMap<>();
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
//$$         this(parent, new CarpetWorkspaceEditor(source, initialPageId));
//$$     }
//$$
//$$     public CarpetWorkspaceScreen(Screen parent, CarpetWorkspaceEditor editor) {
//$$         super(Component.translatable("carpet-gui.screen.title"));
//$$         this.parent = parent;
//$$         this.editor = Objects.requireNonNull(editor, "editor");
//$$     }
//$$
//$$     public CarpetWorkspaceEditor editor() {
//$$         return editor;
//$$     }
//$$
//$$     @Override
//$$     protected void init() {
//$$         valueEditors.clear();
//$$         editor.refreshAllRules();
//$$         int x = Math.max(4, getWidth() / 40);
//$$         int width = Math.max(1, getWidth() - x * 2);
//$$         int bodyHeight = Math.max(1, getHeight() - BODY_Y - 10);
//$$
//$$         TPanelElement frame = panel(x, TAB_Y - 1, width, bodyHeight + BODY_Y - TAB_Y + 1, FRAME);
//$$         addChild(frame);
//$$         initMenus(x, width);
//$$         initTabs(x, width);
//$$
//$$         if (editor.isSelected(GROUPS_ID)) initGroups(x, BODY_Y, width, bodyHeight);
//$$         else if (page() == null) initHome(x, BODY_Y, width, bodyHeight);
//$$         else initRules(page(), x, BODY_Y, width, bodyHeight);
//$$
//$$         notice = label(this, feedback, x + 4, getHeight() - 18, width - 8, 14, MUTED, HorizontalAlignment.LEFT);
//$$         observedRevision = editor.revision();
//$$         this.editor.refresh();
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
//$$                 editor.refresh();
//$$                 editor.refreshAllRules();
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
//$$         UITexture carpetIcon = rootTexture("icon.png");
//$$         actions.add(new LegacyMenuAction(tr("home"), () -> selectPage(null), carpetIcon,
//$$             editor.isSelected(null), !editor.pages().isEmpty()));
//$$         actions.add(new LegacyMenuAction(tr("rule_groups"), () -> selectPage(GROUPS_ID), carpetIcon,
//$$             editor.isSelected(GROUPS_ID), true));
//$$         editor.pages().forEach(page -> actions.add(new LegacyMenuAction(page.title(), () -> selectPage(page.id()),
//$$             pageIcon(page), editor.isSelected(page.id()), false)));
//$$         openMenu(target, actions);
//$$     }
//$$
//$$     private void openMenu(TElement target, List<LegacyMenuAction> actions) {
//$$         TContextMenuPanel popup = new LegacyPopup(target);
//$$         for (LegacyMenuAction action : actions) {
//$$             LegacyMenuItem item = new LegacyMenuItem(action.label(), action.icon(), action.selected());
//$$             item.setSize(Math.max(100, textWidth(action.label()) + (action.icon() == null ? 28 : 38)), 22);
//$$             item.setOnClick(ignored -> {
//$$                 popup.close();
//$$                 action.action().run();
//$$             });
//$$             popup.addChild(item, true);
//$$             if (action.separatorAfter()) popup.addSeparator();
//$$         }
//$$         popup.open();
//$$     }
//$$
//$$     private void initTabs(int x, int width) {
//$$         TPanelElement tabs = panel(x + 1, TAB_Y, width - 2, TAB_HEIGHT, 0x35000000);
//$$         addChild(tabs);
//$$         int tabX = x + 1;
//$$         int homeWidth = 56;
//$$         LegacyChromeButton home = chromeButton(tabX, TAB_Y, homeWidth, TAB_HEIGHT,
//$$             Component.literal("   ").append(tr("home")), () -> selectPage(null), editor.isSelected(null))
//$$             .align(HorizontalAlignment.LEFT);
//$$         add(tabs, home);
//$$         add(home, new TTextureElement(tabX + 5, TAB_Y + 4, 10, 10, rootTexture("icon.png")));
//$$         tabX += homeWidth;
//$$         List<String> tabIds = new ArrayList<>(editor.openedTabs());
//$$         tabIds.sort((left, right) -> Boolean.compare(!GROUPS_ID.equals(left), !GROUPS_ID.equals(right)));
//$$         for (String id : tabIds) {
//$$             Component title;
//$$             if (GROUPS_ID.equals(id)) title = tr("rule_groups");
//$$             else {
//$$                 RulePage candidate = editor.pages().stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);
//$$                 if (candidate == null) continue;
//$$                 title = candidate.title();
//$$             }
//$$             int tabWidth = Math.min(190, Math.max(GROUPS_ID.equals(id) ? 90 : 80, textWidth(title) + 35));
//$$             if (tabX + tabWidth > x + width - 2) break;
//$$             LegacyChromeButton tab = chromeButton(tabX, TAB_Y, tabWidth, TAB_HEIGHT,
//$$                 Component.literal("   ").append(title), () -> selectPage(id), editor.isSelected(id))
//$$                 .align(HorizontalAlignment.LEFT);
//$$             add(tabs, tab);
//$$             add(tab, new TTextureElement(tabX + 5, TAB_Y + 4, 10, 10, rootTexture("icon.png")));
//$$             add(tab, chromeButton(tabX + tabWidth - 15, TAB_Y + 2, 13, TAB_HEIGHT - 4,
//$$                 Component.literal("×"), () -> closeTab(id), false));
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
//$$         int ruleCount = editor.sourcePages().stream().mapToInt(page -> page.rules().size()).sum();
//$$
//$$         int quickY = top + 61;
//$$         label(left, tr("quick_access"), cardX, quickY, cardWidth, 18, ACCENT, HorizontalAlignment.LEFT);
//$$         quickY += 25;
//$$         int columns = Math.max(1, cardWidth / 42);
//$$         int quickRows = Math.max(1, (editor.sourcePages().size() + columns - 1) / columns);
//$$         int quickAccessHeight = quickRows * 42;
//$$         TPanelElement quickCard = panel(cardX - 4, quickY - 4, cardWidth + 8, quickAccessHeight + 2, 0x24000000);
//$$         add(left, quickCard);
//$$         for (int index = 0; index < editor.sourcePages().size(); index++) {
//$$             RulePage page = editor.sourcePages().get(index);
//$$             int tileX = cardX + index % columns * 42;
//$$             int tileY = quickY + index / columns * 42;
//$$             add(quickCard, button(tileX, tileY, 36, 36, Component.empty(), () -> selectPage(page.id()), false)
//$$                 .withTexture(rootTexture("icon.png")));
//$$         }
//$$
//$$         int featuresY = quickY + quickAccessHeight + 16;
//$$         label(left, tr("features"), cardX, featuresY, cardWidth, 18, ACCENT, HorizontalAlignment.LEFT);
//$$         LegacyFeatureCard featureCard = new LegacyFeatureCard(
//$$             cardX - 4, featuresY + 18, cardWidth + 8, 46, () -> selectPage(GROUPS_ID));
//$$         add(left, featureCard);
//$$         add(featureCard, new TTextureElement(cardX + 2, featuresY + 25, 27, 27, rootTexture("icon.png")));
//$$         label(featureCard, tr("rule_groups"), cardX + 36, featuresY + 22, cardWidth - 40, 18,
//$$             TEXT, HorizontalAlignment.LEFT);
//$$         label(featureCard, tr("rule_groups_home"), cardX + 36, featuresY + 40, cardWidth - 42, 14,
//$$             MUTED, HorizontalAlignment.LEFT);
//$$         if (featuresY + 82 < top + height - 12) {
//$$             label(left, tr("news"), cardX, featuresY + 78, cardWidth, 18, ACCENT, HorizontalAlignment.LEFT);
//$$         }
//$$
//$$         int rightX = right.getX() + 12;
//$$         int rightInnerWidth = rightWidth - 24;
//$$         label(right, tr("overview"), rightX, top + 10, rightInnerWidth, 18, ACCENT, HorizontalAlignment.LEFT);
//$$         int rightY = addWrappedText(right, Component.translatable("carpet-gui.workspace.overview_counts",
//$$             editor.sourcePages().stream().filter(candidate -> !candidate.isVanilla()).count(), ruleCount),
//$$             rightX, top + 35, rightInnerWidth, TEXT);
//$$         rightY = addWrappedText(right,
//$$             tr(Minecraft.getInstance().getConnection() == null ? "offline" : "connected"),
//$$             rightX, rightY + 12, rightInnerWidth, MUTED);
//$$         label(right, tr("help"), rightX, rightY + 24, rightInnerWidth, 18, ACCENT, HorizontalAlignment.LEFT);
//$$         addWrappedText(right, tr("help_body"), rightX, rightY + 51, rightInnerWidth, TEXT);
//$$     }
//$$
//$$     private void initRules(RulePage page, int x, int y, int width, int height) {
//$$         RuleBrowserModel model = RuleBrowserModel.forPage(page.id());
//$$         int sidebarWidth = Math.clamp(width * 30 / 100, Math.min(120, width / 3), 235);
//$$         TPanelElement sidebar = panel(x, y, Math.max(1, sidebarWidth - 11), height, PANEL);
//$$         sidebar.setScrollFlags(TPanelElement.SCROLL_VERTICAL);
//$$         sidebar.setSmoothScroll(true);
//$$         sidebar.setScrollPadding(6);
//$$         addChild(sidebar);
//$$         addChild(new LegacyScrollBar(x + sidebarWidth - 10, y, 8, height, sidebar));
//$$
//$$         int sx = x + 9;
//$$         int sw = Math.max(1, sidebarWidth - 29);
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
//$$         add(sidebar, new LegacyControlButton(sx, sy, half, CONTROL_HEIGHT,
//$$             Component.translatable("carpet-gui.expand_all"), () -> {
//$$             model.expandAll(page, true);
//$$             rebuildRuleRows();
//$$         }));
//$$         add(sidebar, new LegacyControlButton(sx + half + 4, sy, sw - half - 4, CONTROL_HEIGHT,
//$$             Component.translatable("carpet-gui.collapse_all"), () -> {
//$$                 model.expandAll(page, false);
//$$                 rebuildRuleRows();
//$$             }));
//$$
//$$         int listX = x + sidebarWidth;
//$$         int listWidth = Math.max(1, width - sidebarWidth);
//$$         counts = label(this, Component.empty(), listX + 10, y + 7, listWidth - 20, 18, MUTED, HorizontalAlignment.LEFT);
//$$         rulePanel = panel(listX + 2, y + 23, Math.max(1, listWidth - 13), Math.max(1, height - 23), PANEL);
//$$         rulePanel.setScrollFlags(TPanelElement.SCROLL_VERTICAL);
//$$         rulePanel.setSmoothScroll(true);
//$$         rulePanel.setScrollPadding(6);
//$$         addChild(rulePanel);
//$$         addChild(new LegacyScrollBar(listX + listWidth - 10, y + 23, 8, Math.max(1, height - 23), rulePanel));
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
//$$         add(left, new LegacyControlButton(x + 9, y + 34, leftWidth - 18, CONTROL_HEIGHT,
//$$             tr(creatingGroup ? "cancel" : "new_group"), () -> {
//$$             creatingGroup = !creatingGroup;
//$$             rebuild();
//$$         }));
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
//$$             add(left, new LegacyControlButton(x + 9, gy, leftWidth - 18, CONTROL_HEIGHT, tr("create_group"), () -> {
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
//$$             }));
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
//$$             label(left, fitText(Component.literal(group.name().isBlank() ? group.id() : group.name()), leftWidth - 32),
//$$                 x + 16, gy + 3, leftWidth - 32, 12, selectedGroup ? ACCENT : TEXT, HorizontalAlignment.LEFT);
//$$             String detail = (group.tag().isBlank() ? "" : group.tag() + " · ") + "r" + group.revision()
//$$                 + (group.commitsAhead() > 0 ? " ↑" + group.commitsAhead() : " ✓");
//$$             label(left, Component.literal(detail), x + 16, gy + 17, leftWidth - 32, 10, MUTED, HorizontalAlignment.LEFT);
//$$             gy += ROW_HEIGHT + 3;
//$$         }
//$$         if (groups.isEmpty()) addWrappedText(left, tr("no_groups"), x + 10, gy + 5, leftWidth - 20, MUTED);
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
//$$         label(center, fitText(Component.literal(group.name()), Math.max(1, width - 105)),
//$$             x, y + 8, Math.max(1, width - 105), 18, ACCENT, HorizontalAlignment.LEFT);
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
//$$             addWrappedText(center, tr("empty_group"), x, rowY + 8, width, MUTED);
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
//$$         add(center, new LegacyScrollBar(x + width - 8, y + 33, 8, Math.max(1, height - 42), groupCandidates));
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
//$$         for (RulePage page : editor.sourcePages()) {
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
//$$         for (RulePage page : editor.sourcePages()) {
//$$             for (RuleView rule : page.rules()) {
//$$                 if (!needle.isEmpty() && rule.searchTerms().stream()
//$$                     .map(term -> term.toLowerCase(Locale.ROOT))
//$$                     .noneMatch(term -> term.contains(needle))) continue;
//$$                 boolean member = activeGroup.members().contains(rule.stateId());
//$$                 Component ruleLabel = Component.literal((member ? "− " : "+ ") + rule.label().getString());
//$$                 add(groupCandidates, button(rx + 4, cy, rw - 8, CONTROL_HEIGHT,
//$$                     fitText(ruleLabel, Math.max(1, rw - 20)), () -> {
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
//$$             boolean expanded = model.expanded(group.category());
//$$             LegacyCategoryHeader header = new LegacyCategoryHeader(x, y, width, 20, () -> {
//$$                 model.setExpanded(group.category(), !model.expanded(group.category()));
//$$                 rebuildRuleRows();
//$$             });
//$$             add(rulePanel, header);
//$$             label(header, title, x + 4, y, width - 34, 20, ACCENT, HorizontalAlignment.LEFT);
//$$             label(header, Component.literal(expanded ? "[-]" : "[+]"),
//$$                 x + width - 28, y, 24, 20, MUTED, HorizontalAlignment.RIGHT);
//$$             y += 23;
//$$             if (!expanded) continue;
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
//$$         label(row, fitText(rule.label(), textWidth), x + 5, y + 2, textWidth, 12,
//$$             rule.differsFromConfiguredValue() ? ACCENT : TEXT, HorizontalAlignment.LEFT);
//$$         label(row, fitText(rule.description(), textWidth), x + 5, y + 15, textWidth, 11, MUTED, HorizontalAlignment.LEFT);
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
//$$             LegacyDropdown value = dropdown(valueX, y + 4, valueWidth, rule.value(), valueOptions,
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
//$$         this.editor.refresh();
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
//$$         return editor.selectedPage();
//$$     }
//$$
//$$     private void selectPage(String id) {
//$$         editor.selectPage(id);
//$$         feedback = Component.empty();
//$$         rebuild();
//$$     }
//$$
//$$     private void closeTab(String id) {
//$$         editor.closePage(id);
//$$         feedback = Component.empty();
//$$         rebuild();
//$$     }
//$$
//$$     private void cyclePage() {
//$$         if (editor.isSelected(GROUPS_ID)) {
//$$             selectPage(null);
//$$             return;
//$$         }
//$$         RulePage current = page();
//$$         if (current == null) selectPage(editor.pages().isEmpty() ? GROUPS_ID : editor.pages().getFirst().id());
//$$         else {
//$$             int index = editor.pages().indexOf(current);
//$$             selectPage(index + 1 < editor.pages().size() ? editor.pages().get(index + 1).id() : GROUPS_ID);
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
//$$             editor.refresh();
//$$             if (editor.revision() != observedRevision && getFocusedElement() == null) {
//$$                 observedRevision = editor.revision();
//$$                 editor.refreshAllRules();
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
//$$     private LegacyChromeButton chromeButton(int x, int y, int width, int height, Component text,
//$$                                             Runnable action, boolean selected) {
//$$         Component label = selected ? text.copy().withStyle(ChatFormatting.YELLOW) : text;
//$$         return new LegacyChromeButton(x, y, Math.max(1, width), Math.max(1, height), label, action, selected);
//$$     }
//$$
//$$     private LegacyButton iconButton(int x, int y, int width, int height, String icon,
//$$                                     Runnable action, boolean selected) {
//$$         return button(x, y, width, height, Component.empty(), action, selected).withIcon(icon);
//$$     }
//$$
//$$     private <T> LegacyDropdown dropdown(int x, int y, int width, T selected,
//$$                                         List<LegacyOption<T>> options, Consumer<T> changed) {
//$$         LegacyDropdown select = new LegacyDropdown(x, y, Math.max(1, width), CONTROL_HEIGHT);
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
//$$     private static UITexture pageIcon(RulePage page) {
//$$         return "minecraft".equals(page.id())
//$$             ? new UITexture(ResourceLocation.withDefaultNamespace("textures/block/grass_block_side.png"))
//$$             : rootTexture("icon.png");
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
//$$     private Component fitText(Component text, int maxWidth) {
//$$         if (maxWidth <= 0) return Component.empty();
//$$         if (textWidth(text) <= maxWidth) return text;
//$$         String ellipsis = "…";
//$$         if (textWidth(Component.literal(ellipsis)) > maxWidth) return Component.empty();
//$$         String source = text.getString();
//$$         StringBuilder fitted = new StringBuilder();
//$$         for (int offset = 0; offset < source.length();) {
//$$             int codePoint = source.codePointAt(offset);
//$$             String character = new String(Character.toChars(codePoint));
//$$             if (textWidth(Component.literal(fitted + character + ellipsis)) > maxWidth) break;
//$$             fitted.append(character);
//$$             offset += Character.charCount(codePoint);
//$$         }
//$$         return Component.literal(fitted + ellipsis).withStyle(text.getStyle());
//$$     }
//$$
//$$     private int addWrappedText(TParentElement parent, Component text, int x, int y, int width, int color) {
//$$         int lineY = y;
//$$         for (String line : wrapLines(text.getString(), Math.max(1, width))) {
//$$             label(parent, Component.literal(line), x, lineY, Math.max(1, width), 11,
//$$                 color, HorizontalAlignment.LEFT);
//$$             lineY += 12;
//$$         }
//$$         return lineY;
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
//$$     private record LegacyMenuAction(Component label, Runnable action, UITexture icon,
//$$                                     boolean selected, boolean separatorAfter) {
//$$         LegacyMenuAction(Component label, Runnable action) {
//$$             this(label, action, null, false, false);
//$$         }
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
//$$         protected final boolean selected;
//$$         protected HorizontalAlignment alignment = HorizontalAlignment.CENTER;
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
//$$     private static final class LegacyChromeButton extends LegacyButton {
//$$         LegacyChromeButton(int x, int y, int width, int height, Component text, Runnable action, boolean selected) {
//$$             super(x, y, width, height, text, action, selected);
//$$         }
//$$
//$$         @Override
//$$         LegacyChromeButton align(HorizontalAlignment alignment) {
//$$             super.align(alignment);
//$$             return this;
//$$         }
//$$
//$$         @Override
//$$         public void render(TDrawContext graphics) {
//$$             if (selected || isFocusedOrHovered()) {
//$$                 graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
//$$                     selected ? 0xFF414345 : 0xA0454545);
//$$             }
//$$             if (selected) {
//$$                 graphics.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(),
//$$                     getY() + getHeight(), 0xFF4288B8);
//$$             }
//$$             if (getIcon() != null) renderIcon(graphics);
//$$             else graphics.drawTElementTextTHC(getText(), alignment, getEnabled() ? TEXT : MUTED);
//$$         }
//$$     }
//$$
//$$     private static final class LegacyControlButton extends TButtonWidget {
//$$         LegacyControlButton(int x, int y, int width, int height, Component text, Runnable action) {
//$$             super(x, y, width, height, text, ignored -> action.run());
//$$         }
//$$     }
//$$
//$$     private static final class LegacyFeatureCard extends LegacyButton {
//$$         LegacyFeatureCard(int x, int y, int width, int height, Runnable action) {
//$$             super(x, y, width, height, Component.empty(), action, false);
//$$         }
//$$
//$$         @Override
//$$         public void render(TDrawContext graphics) {
//$$             graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
//$$                 isFocusedOrHovered() ? 0x50393939 : 0x24000000);
//$$             if (!isFocusedOrHovered()) return;
//$$             graphics.fill(getX(), getY(), getX() + getWidth(), getY() + 1, FOCUS);
//$$             graphics.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), FOCUS);
//$$             graphics.fill(getX(), getY(), getX() + 1, getY() + getHeight(), FOCUS);
//$$             graphics.fill(getX() + getWidth() - 1, getY(), getX() + getWidth(), getY() + getHeight(), FOCUS);
//$$         }
//$$     }
//$$
//$$     private static final class LegacyCategoryHeader extends LegacyButton {
//$$         LegacyCategoryHeader(int x, int y, int width, int height, Runnable action) {
//$$             super(x, y, width, height, Component.empty(), action, false);
//$$         }
//$$
//$$         @Override
//$$         public void render(TDrawContext graphics) {
//$$             graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
//$$                 isFocusedOrHovered() ? 0xFF607994 : 0x22000000);
//$$             graphics.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(),
//$$                 isFocusedOrHovered() ? 0xFF9EACBC : 0x40707070);
//$$         }
//$$     }
//$$
//$$     private static final class LegacyPopup extends TContextMenuPanel {
//$$         LegacyPopup(TElement target) {
//$$             super(target);
//$$         }
//$$
//$$         @Override
//$$         public void render(TDrawContext graphics) {
//$$             int x = getX();
//$$             int y = getY();
//$$             int width = getWidth();
//$$             int height = getHeight();
//$$             if (width < 5 || height < 5) {
//$$                 graphics.fill(x, y, x + width, y + height, POPUP_BACKGROUND);
//$$                 return;
//$$             }
//$$             graphics.fill(x + 2, y, x + width - 2, y + 1, POPUP_BORDER);
//$$             graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, POPUP_BORDER);
//$$             graphics.fill(x, y + 2, x + width, y + height - 2, POPUP_BORDER);
//$$             graphics.fill(x + 2, y + 1, x + width - 2, y + height - 1, POPUP_BACKGROUND);
//$$             graphics.fill(x + 1, y + 2, x + width - 1, y + height - 2, POPUP_BACKGROUND);
//$$         }
//$$
//$$         @Override
//$$         public void postRender(TDrawContext graphics) {
//$$         }
//$$     }
//$$
//$$     private static final class LegacyScrollBar extends TScrollBarWidget {
//$$         LegacyScrollBar(int x, int y, int width, int height, TPanelElement target) {
//$$             super(x, y, width, height, target);
//$$         }
//$$
//$$         @Override
//$$         public void renderSliderProgressBar(TDrawContext graphics) {
//$$             // A scrollbar has one continuous track; do not paint its traversed area as slider progress.
//$$         }
//$$     }
//$$
//$$     private static final class LegacyMenuItem extends TMenuPanelButton {
//$$         private final UITexture icon;
//$$         private final boolean selected;
//$$
//$$         LegacyMenuItem(Component text, UITexture icon, boolean selected) {
//$$             super(text);
//$$             this.icon = icon;
//$$             this.selected = selected;
//$$         }
//$$
//$$         @Override
//$$         public void render(TDrawContext graphics) {
//$$             if (isFocusedOrHovered()) {
//$$                 graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), POPUP_HOVER);
//$$             }
//$$             int textInset = 6;
//$$             if (icon != null) {
//$$                 icon.drawTexture(graphics, getX() + 3, getY() + 5, 12, 12);
//$$                 textInset = 21;
//$$             }
//$$             graphics.drawTElementTextTHSC(getText(), HorizontalAlignment.LEFT, textInset,
//$$                 selected ? ACCENT : TEXT);
//$$         }
//$$
//$$         @Override
//$$         public void postRender(TDrawContext graphics) {
//$$         }
//$$     }
//$$
//$$     private final class LegacyDropdown extends TSelectWidget<TSelectWidget.SimpleEntry> {
//$$         LegacyDropdown(int x, int y, int width, int height) {
//$$             super(x, y, width, height, new TSelectWidget.SimpleEntry[0]);
//$$         }
//$$
//$$         @Override
//$$         public TContextMenuPanel createContextMenu() {
//$$             TContextMenuPanel popup = new LegacyPopup(this);
//$$             for (TSelectWidget.SimpleEntry entry : this) {
//$$                 LegacyMenuItem item = new LegacyMenuItem(entry.getText(), null, entry == getSelected());
//$$                 item.setSize(Math.max(getWidth(), textWidth(entry.getText()) + 28), 22);
//$$                 item.setOnClick(ignored -> {
//$$                     setSelected(entry);
//$$                     popup.close();
//$$                 });
//$$                 popup.addChild(item, true);
//$$             }
//$$             return popup;
//$$         }
//$$     }
//$$
//$$ }
//#endif

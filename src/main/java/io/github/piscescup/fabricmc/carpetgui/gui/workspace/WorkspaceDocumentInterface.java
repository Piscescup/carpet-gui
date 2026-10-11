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
import com.thecsdev.commonmc.api.client.gui.ctxmenu.TContextMenu;
import com.thecsdev.commonmc.api.client.gui.label.TLabelElement;
import com.thecsdev.commonmc.api.client.gui.panel.TPanelElement;
import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
import com.thecsdev.commonmc.api.client.gui.util.TInputContext;
import com.thecsdev.commonmc.api.client.gui.widget.TClickableWidget;
import com.thecsdev.commonmc.api.client.gui.widget.TCheckboxWidget;
import io.github.piscescup.fabricmc.carpetgui.api.RuleView;
import io.github.piscescup.fabricmc.carpetgui.gui.CarpetWorkspaceScreen;
import io.github.piscescup.fabricmc.carpetgui.gui.model.*;
import io.github.piscescup.fabricmc.carpetgui.gui.pages.RulePage;
import io.github.piscescup.fabricmc.carpetgui.gui.GUIStyle;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.NativeTextInput;
import io.github.piscescup.fabricmc.carpetgui.network.ClientRuleConfigurations;
import io.github.piscescup.fabricmc.carpetgui.store.ModIconStore;
import io.github.piscescup.fabricmc.carpetgui.util.MsgUtils;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;

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
        modifiedFilter = null;
        savedDefaultFilter = null;
        var screen = getBounds();
        int x = Math.max(4, screen.width / 40);
        int width = Math.max(1, screen.width - 2 * x);
        int bodyY = GUIStyle.BODY_Y;
        int bodyHeight = Math.max(1, screen.height - bodyY - 10);
        var frame = new TPanelElement.Paintable(GUIStyle.FRAME_COLOR, GUIStyle.BORDER_COLOR, GUIStyle.BORDER_COLOR);
        frame.setBounds(x, GUIStyle.TAB_Y - 1, width, bodyY + bodyHeight - GUIStyle.TAB_Y + 1);
        frame.hoverableProperty().set(false, WorkspaceDocumentInterface.class);
        frame.focusableProperty().set(false, WorkspaceDocumentInterface.class);
        add(frame);
        buildTabStrip(x, width);
        contentRoot = WorkspacePageElement.create(this, editor);
        contentRoot.setBounds(x, bodyY, width, bodyHeight);
        add(contentRoot);
        notice = WorkspaceStyle.label(this, Component.empty(), x + 4, screen.height - 18, width - 8, 14, GUIStyle.MUTED_COLOR);
        notice.wrapTextProperty()
            .set(false, WorkspaceDocumentInterface.class);
        observedRevision = editor.revision();
        observedNavigationRevision = editor.navigationRevision();
        listDirty = false;
    }

    private void buildTabStrip(int x, int width) {
        var tabs = new WorkspaceTabStripPanel(editor);
        tabs.setBounds(x + 1, GUIStyle.TAB_Y, Math.max(1, width - 2), GUIStyle.TAB_HEIGHT);
        add(tabs);
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

    CarpetWorkspaceScreen screen() {
        return screen;
    }

    void clearFocusedElement() {
        screen.focusedElementProperty().set(null, WorkspaceDocumentInterface.class);
    }

    void registerRuleListState(RuleListPanel list, TLabelElement counts, List<String> categories) {
        this.ruleList = list;
        this.counts = counts;
        this.observedCategories = List.copyOf(categories);
    }

    void registerRuleFilters(TCheckboxWidget modified, TCheckboxWidget savedDefault) {
        this.modifiedFilter = modified;
        this.savedDefaultFilter = savedDefault;
    }

    Component categoryLabel(RulePage page, String category) {
        if (category.equals(RuleBrowserModel.ALL_RULES)) return Component.translatable("carpet-gui.tab.all");
        if (category.equals(RuleBrowserModel.FAVORITES)) return MsgUtils.tr("favorites");
        return category.equals(RuleBrowserModel.UNCATEGORIZED) ? MsgUtils.tr("uncategorized") : page.categoryLabel(category);
    }

    void selectPage(String id) {
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

    void requestListRefresh(boolean reset) {
        listDirty = true;
        resetScroll |= reset;
    }

    void expandAll(boolean expanded) {
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
        return (int) Math.ceil(getClient().font.width(text) * GUIStyle.TEXT_SCALE);
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
        if (contentRoot instanceof WorkspaceHomePageElement home) home.refreshOverview();
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
        graphics.fillColor(0, 0, bounds.width, bounds.height, GUIStyle.BACKDROP_COLOR);
    }



}
//#endif
//#if MC <= 12110
//$$ package io.github.piscescup.fabricmc.carpetgui.gui.workspace;
//$$
//$$ /** Legacy document surface; behavior lives in the shared legacy controller implementation. */
//$$ public final class WorkspaceDocumentInterface extends LegacyWorkspaceDocumentController {
//$$     public WorkspaceDocumentInterface(CarpetWorkspaceScreen screen, CarpetWorkspaceEditor editor) {
//$$         super(screen, editor);
//$$     }
//$$ }
//#endif


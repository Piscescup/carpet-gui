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

package io.github.piscescup.fabricmc.carpetgui.gui.workspace;

import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleBrowserModel;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RulePage;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleSource;
import io.github.piscescup.fabricmc.carpetgui.gui.pages.AllRulesPage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Owns one workspace session's data source, navigation pages and open-tab state.
 * Rendering and widget lifetime remain the responsibility of the screen.
 */
public final class CarpetWorkspaceEditor {
    public static final String RULE_GROUPS_PAGE_ID = "carpet-gui:rule-groups";

    private final RuleSource source;
    private final List<? extends RulePage> sourcePages;
    private final AllRulesPage allRules;
    private final List<? extends RulePage> pages;
    private final Set<String> openedTabs = new LinkedHashSet<>();
    private final Set<String> openedTabsView = Collections.unmodifiableSet(openedTabs);
    private String selectedPageId;
    private long navigationRevision;

    public CarpetWorkspaceEditor(RuleSource source, String initialPageId) {
        this.source = Objects.requireNonNull(source, "source");
        sourcePages = List.copyOf(source.pages());
        allRules = new AllRulesPage(sourcePages);

        var navigationPages = new ArrayList<RulePage>(sourcePages.size() + 1);
        navigationPages.add(allRules);
        navigationPages.addAll(sourcePages);
        pages = List.copyOf(navigationPages);

        openedTabs.add(AllRulesPage.ID);
        selectInitialPage(initialPageId);
        pages.forEach(page -> RuleBrowserModel.forPage(page.id()).invalidateSearch());
    }

    public RuleSource source() {
        return source;
    }

    public List<? extends RulePage> sourcePages() {
        return sourcePages;
    }

    public List<? extends RulePage> pages() {
        return pages;
    }

    public Set<String> openedTabs() {
        return openedTabsView;
    }

    public String selectedPageId() {
        return selectedPageId;
    }

    public boolean isSelected(String pageId) {
        return Objects.equals(selectedPageId, pageId);
    }

    public boolean isPageOpen(String pageId) {
        return openedTabs.contains(pageId);
    }

    public RulePage selectedPage() {
        return pages.stream()
            .filter(page -> page.id().equals(selectedPageId))
            .findFirst()
            .orElse(null);
    }

    public void selectPage(String pageId) {
        if (!isKnownPage(pageId)) {
            throw new IllegalArgumentException("Unknown workspace page: " + pageId);
        }
        boolean changed = !Objects.equals(selectedPageId, pageId) || pageId != null && !openedTabs.contains(pageId);
        selectedPageId = pageId;
        if (pageId != null) openedTabs.add(pageId);
        if (changed) navigationRevision++;
    }

    public void closePage(String pageId) {
        if (pageId == null) return;
        boolean changed = openedTabs.remove(pageId);
        if (Objects.equals(selectedPageId, pageId)) {
            selectedPageId = null;
            changed = true;
        }
        if (changed) navigationRevision++;
    }

    public void refresh() {
        source.refresh();
    }

    /** Refreshes workspace data and asks the screen to rebuild on its next tick. */
    public void refreshWorkspace() {
        source.refresh();
        allRules.refreshIndex();
        navigationRevision++;
    }

    public long revision() {
        return source.revision();
    }

    public long navigationRevision() {
        return navigationRevision;
    }

    public void refreshAllRules() {
        allRules.refreshIndex();
    }

    private void selectInitialPage(String pageId) {
        selectedPageId = isKnownPage(pageId) ? pageId : null;
        if (selectedPageId != null) openedTabs.add(selectedPageId);
    }

    private boolean isKnownPage(String pageId) {
        return pageId == null || RULE_GROUPS_PAGE_ID.equals(pageId) || pages.stream()
            .anyMatch(page -> page.id().equals(pageId));
    }
}

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
import com.thecsdev.commonmc.api.client.gui.misc.TTextureElement;
import com.thecsdev.commonmc.api.client.gui.panel.TPanelElement;
import com.thecsdev.commonmc.api.client.gui.tooltip.TTooltip;
import io.github.piscescup.fabricmc.carpetgui.References;
import io.github.piscescup.fabricmc.carpetgui.gui.GUIStyle;
import io.github.piscescup.fabricmc.carpetgui.gui.pages.RulePage;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.button.Button;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.button.IconButton;
import io.github.piscescup.fabricmc.carpetgui.store.ModIconStore;
import io.github.piscescup.fabricmc.carpetgui.util.MsgUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Home-page content and its live overview labels.
 */
final class WorkspaceHomePageElement
    extends WorkspacePageElement
{
    private final CarpetWorkspaceEditor editor;
    private TLabelElement homeCounts;
    private TLabelElement connectionStatus;
    private TLabelElement homeHelpHeading;
    private TLabelElement homeHelpBody;

    WorkspaceHomePageElement(WorkspaceDocumentInterface workspace, CarpetWorkspaceEditor editor) {
        super(workspace);
        this.editor = editor;
    }

    @Override
    protected void initCallback() {
        var bounds = getBounds();
        int x = bounds.x + 12, y = bounds.y + 12;
        int width = Math.max(1, bounds.width - 24), height = Math.max(1, bounds.height - 24);
        int leftWidth = Math.max(1, width * 2 / 3 - 5);
        var left = new WorkspacePanel(8);
        var right = new WorkspacePanel(12);
        workspace.addWorkspacePane(left, x, y, leftWidth, height);
        workspace.addWorkspacePane(right, x + leftWidth + 8, y, Math.max(1, width - leftWidth - 8), height);

        int contentX = x + 12, contentWidth = Math.max(1, left.getBounds().width - 24), nextY = y + 12;
        addHomeCard(left, contentX - 4, nextY - 4, contentWidth + 8, 40);
        WorkspaceStyle.label(
            left, Component.literal("Carpet GUI"), contentX + 38, nextY + 1,
            contentWidth - 38, 13, GUIStyle.ACCENT_COLOR
        );
        WorkspaceStyle.label(
            left, Component.literal(References.MOD_VERSION), contentX + 38, nextY + 18,
            contentWidth - 38, 13, GUIStyle.MUTED_COLOR
        );
        addIcon(left, "carpet-gui", contentX, nextY, 30);
        nextY += 51;

        WorkspaceStyle.label(left, MsgUtils.tr("quick_access"), contentX, nextY, contentWidth, 16, GUIStyle.ACCENT_COLOR);
        nextY += 25;
        int columns = Math.max(1, contentWidth / 42);
        int quickAccessHeight = Math.max(
            1,
            (editor.sourcePages()
                 .size() + columns - 1) / columns
        ) * 42;
        addHomeCard(left, contentX - 4, nextY - 4, contentWidth + 8, quickAccessHeight + 2);
        for (int index = 0; index < editor.sourcePages()
            .size(); index++) {
            RulePage page = editor.sourcePages()
                .get(index);
            int tileX = contentX + (index % columns) * 42, tileY = nextY + (index / columns) * 42;
            var tile = new IconButton(() -> workspace.selectPage(page.id())) {
                @Override
                protected void initCallback() {
                    super.initCallback();
                    var tileBounds = getBounds();
                    addIcon(this, page.id(), tileBounds.x + 5, tileBounds.y + 5, 26);
                }
            };
            tile.setBounds(tileX, tileY, 36, 36);
            tile.tooltipProperty()
                .set(ignored -> TTooltip.of(page.title()), WorkspaceHomePageElement.class);
            left.add(tile);
        }

        nextY += quickAccessHeight + 16;
        WorkspaceStyle.label(left, MsgUtils.tr("features"), contentX, nextY, contentWidth, 16, GUIStyle.ACCENT_COLOR);
        nextY += 22;
        addHomeCard(left, contentX - 4, nextY - 4, contentWidth + 8, 46);
        var groupsFeature = new Button(MsgUtils.tr("rule_groups"), () -> workspace.selectPage(RuleGroupWorkspace.ID)) {
            @Override
            protected void initCallback() {
                super.initCallback();
                var featureBounds = getBounds();
                getLabel().setBounds(
                    featureBounds.x + 39, featureBounds.y + 4,
                    Math.max(1, featureBounds.width - 45), 14
                );
                getLabel().textAlignmentProperty()
                    .set(CompassDirection.WEST, WorkspaceHomePageElement.class);
                var description = WorkspaceStyle.label(
                    this, MsgUtils.tr("rule_groups_home"), featureBounds.x + 39,
                    featureBounds.y + 21, Math.max(1, featureBounds.width - 45), 11, GUIStyle.MUTED_COLOR
                );
                description.textScaleProperty()
                    .set(GUIStyle.SMALL_TEXT_SCALE, WorkspaceHomePageElement.class);
                description.wrapTextProperty()
                    .set(false, WorkspaceHomePageElement.class);
                addIcon(this, "carpet-gui", featureBounds.x + 6, featureBounds.y + 7, 27);
            }
        };
        groupsFeature.setBounds(contentX, nextY, contentWidth, 38);
        groupsFeature.tooltipProperty()
            .set(
                ignored -> TTooltip.of(MsgUtils.tr("rule_groups_home")),
                WorkspaceHomePageElement.class
            );
        left.add(groupsFeature);

        nextY += 56;
        WorkspaceStyle.label(left, MsgUtils.tr("news"), contentX, nextY, contentWidth, 18, GUIStyle.ACCENT_COLOR);

        var rightBounds = right.getBounds();
        int rightX = rightBounds.x + 12, rightWidth = Math.max(1, rightBounds.width - 24), rightY = y + 12;
        WorkspaceStyle.label(right, MsgUtils.tr("overview"), rightX, rightY, rightWidth, 18, GUIStyle.ACCENT_COLOR);
        homeCounts = WorkspaceStyle.label(right, overviewCounts(), rightX, rightY + 26, rightWidth, 40, GUIStyle.TEXT_COLOR);
        connectionStatus = WorkspaceStyle.label(
            right,
            MsgUtils.tr(getClient().getConnection() == null ? "offline" : "connected"),
            rightX, rightY + 72, rightWidth, 33, GUIStyle.MUTED_COLOR
        );
        homeHelpHeading = WorkspaceStyle.label(
            right, MsgUtils.tr("help"), rightX, rightY + 125,
            rightWidth, 18, GUIStyle.ACCENT_COLOR
        );
        homeHelpBody = WorkspaceStyle.label(
            right, MsgUtils.tr("help_body"), rightX, rightY + 152,
            rightWidth, 10, GUIStyle.TEXT_COLOR
        );
        homeCounts.wrapTextProperty()
            .set(true, WorkspaceHomePageElement.class);
        connectionStatus.wrapTextProperty()
            .set(true, WorkspaceHomePageElement.class);
        homeHelpBody.wrapTextProperty()
            .set(true, WorkspaceHomePageElement.class);
        layoutHomeOverview();
    }

    void refreshOverview() {
        if (homeCounts == null) return;
        homeCounts.setText(overviewCounts());
        connectionStatus.setText(MsgUtils.tr(getClient().getConnection() == null ? "offline" : "connected"));
        layoutHomeOverview();
    }

    private Component overviewCounts() {
        return Component.translatable(
            "carpet-gui.workspace.overview_counts",
            editor.sourcePages()
                .stream()
                .filter(page -> !page.isVanilla())
                .count(),
            editor.sourcePages()
                .stream()
                .mapToInt(page -> page.rules()
                    .size())
                .sum()
        );
    }

    private void addHomeCard(TElement parent, int x, int y, int width, int height) {
        var card = new TPanelElement.Paintable(GUIStyle.CARD_COLOR, 0, 0);
        card.setBounds(x, y, width, height);
        card.hoverableProperty()
            .set(false, WorkspaceHomePageElement.class);
        card.focusableProperty()
            .set(false, WorkspaceHomePageElement.class);
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

    private static void addIcon(TElement parent, String modId, int x, int y, int size) {
        Identifier icon = "minecraft".equals(modId)
            ? Identifier.withDefaultNamespace("textures/block/grass_block_side.png")
            : ModIconStore.INSTANCE.icon(modId)
                .orElse(null);
        if (icon != null) {
            var texture = new TTextureElement(icon);
            texture.modeProperty()
                .set(TTextureElement.Mode.TEXTURE, WorkspaceHomePageElement.class);
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
                GUIStyle.ACCENT_COLOR
            );
            fallback.textAlignmentProperty()
                .set(CompassDirection.CENTER, WorkspaceHomePageElement.class);
            fallback.textScaleProperty()
                .set(Math.max(1.0, size / 15.0), WorkspaceHomePageElement.class);
        }
    }
}
//#endif

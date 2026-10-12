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

package io.github.piscescup.fabricmc.carpetgui.gui.workspace;

//#if MC >= 12111
import com.thecsdev.commonmc.api.client.gui.tooltip.TTooltip;
//#endif
import io.github.piscescup.fabricmc.carpetgui.api.EditableRuleView;
import io.github.piscescup.fabricmc.carpetgui.api.RuleView;
import io.github.piscescup.fabricmc.carpetgui.network.ClientRuleChanges;
import io.github.piscescup.fabricmc.carpetgui.network.RuleChangeEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Builds rule hover content while history remains owned by {@link ClientRuleChanges}. */
final class WorkspaceRuleTooltip {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    private WorkspaceRuleTooltip() {}

    //#if MC >= 12111
    static TTooltip create(String modId, RuleView rule) {
        return TTooltip.of(text(modId, rule));
    }
    //#endif

    static Component text(String modId, RuleView rule) {
        String key = rule.id().contains(":") ? rule.id() : modId + ":" + rule.id();
        var text = rule.label()
            .copy()
            .withStyle(ChatFormatting.YELLOW)
            .append(Component.literal("\nKey: " + key + "\nValue: " + rule.value())
                .withStyle(ChatFormatting.GRAY));
        if (!rule.description().getString().isBlank()) {
            text.append(Component.literal("\n\n"))
                .append(rule.description().copy().withStyle(ChatFormatting.WHITE));
        }
        for (Component info : rule.extraInfo()) {
            text.append(Component.literal("\n"))
                .append(info.copy().withStyle(ChatFormatting.GRAY));
        }
        if (rule instanceof EditableRuleView editable && !editable.editor().editable()) {
            text.append(Component.literal("\n"))
                .append(editable.editor().disabledReason().copy().withStyle(ChatFormatting.RED));
        }
        appendHistory(text, ClientRuleChanges.history(rule.stateId()));
        return text;
    }

    /** History is newest first; consequently earlier changes appear lower in the tooltip. */
    private static void appendHistory(MutableComponent text, List<RuleChangeEvent> history) {
        if (history.isEmpty()) return;
        text.append(Component.literal("\n\n"))
            .append(Component.translatable("carpet-gui.change_history").withStyle(ChatFormatting.YELLOW));
        for (RuleChangeEvent event : history) {
            String time = TIME.format(Instant.ofEpochMilli(event.changedAt()).atZone(ZoneId.systemDefault()));
            text.append(Component.literal("\n" + event.actorName() + ": ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(event.oldValue() + " -> " + event.newValue()).withStyle(ChatFormatting.WHITE))
                .append(Component.translatable("carpet-gui.change_time", time).withStyle(ChatFormatting.GRAY));
        }
    }
}

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

//#if MC >= 260000
package io.github.piscescup.fabricmc.carpetgui.gui.model;

import io.github.piscescup.fabricmc.carpetgui.api.EditableRuleView;
import io.github.piscescup.fabricmc.carpetgui.api.RuleEditor;
import io.github.piscescup.fabricmc.carpetgui.integration.RuleCommandGateway;
import io.github.piscescup.fabricmc.carpetgui.store.VanillaRuleStore;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleType;

import java.util.List;

public final class VanillaRuleView
    implements EditableRuleView, RuleEditor
{
    private final ResourceKey<GameRule<?>> key;
    private final GameRule<?> rule;
    private final RuleCommandGateway gateway;

    public VanillaRuleView(ResourceKey<GameRule<?>> key, GameRule<?> rule, RuleCommandGateway gateway) {
        this.key = key;
        this.rule = rule;
        this.gateway = gateway;
    }

    @Override
    public String id() {
        return key.identifier()
            .toString();
    }

    @Override
    public Component label() {
        return Component.translatableWithFallback(rule.getDescriptionId(), id());
    }

    @Override
    public Component description() {
        return Component.translatableWithFallback(rule.getDescriptionId() + ".description", "");
    }

    @Override
    public String value() {
        return VanillaRuleStore.values()
            .getOrDefault(key, "?");
    }

    @Override
    public String defaultValue() {
        return defaultString(rule);
    }

    private static <T> String defaultString(GameRule<T> rule) {
        return rule.serialize(rule.defaultValue());
    }

    @Override
    public List<String> categories() {
        return List.of(rule.category()
            .id()
            .getPath());
    }

    @Override
    public RuleEditor editor() {
        return this;
    }

    @Override
    public InputKind inputKind() {
        return rule.gameRuleType() == GameRuleType.BOOL ? InputKind.BOOLEAN : InputKind.NUMBER;
    }

    @Override
    public List<String> suggestions() {
        return inputKind() == InputKind.BOOLEAN ? List.of("true", "false") : List.of();
    }

    @Override
    public boolean strict() {
        return inputKind() == InputKind.BOOLEAN;
    }

    @Override
    public boolean editable() {
        return VanillaRuleStore.values()
                   .containsKey(key) && gateway.canExecute("gamerule");
    }

    @Override
    public Component disabledReason() {
        return Component.translatable("carpet-gui.edit.no_permission");
    }

    @Override
    public RuleEditResult submit(String value) {
        if (!editable()) return RuleEditResult.rejected(disabledReason());
        if (value.length() > 256 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0 || value.indexOf('\0') >= 0
            || (strict() && !suggestions().contains(value)) || rule.deserialize(value)
                .result()
                .isEmpty()) {
            return RuleEditResult.rejected(Component.translatable("carpet-gui.edit.invalid_value"));
        }
        return gateway.send("gamerule " + id() + " " + value);
    }
}
//#endif

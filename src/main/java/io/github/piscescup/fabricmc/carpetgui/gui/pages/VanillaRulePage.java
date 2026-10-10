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

package io.github.piscescup.fabricmc.carpetgui.gui.pages;

import io.github.piscescup.fabricmc.carpetgui.api.RuleView;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Supplier;

/**
 * Minecraft gamerules are a separate source tab, not a filter on every Carpet mod.
 */
public final class VanillaRulePage
    implements RulePage
{
    private final Supplier<List<? extends RuleView>> rules;

    public VanillaRulePage(Supplier<List<? extends RuleView>> rules) {
        this.rules = rules;
    }

    @Override
    public String id() {
        return "minecraft";
    }

    @Override
    public Component title() {
        return Component.literal("Minecraft");
    }

    @Override
    public List<? extends RuleView> rules() {
        return rules.get();
    }

    @Override
    public boolean isVanilla() {
        return true;
    }

    @Override
    public Component categoryLabel(String category) {
        return Component.translatableWithFallback("gamerule.category.minecraft." + category, category);
    }
}

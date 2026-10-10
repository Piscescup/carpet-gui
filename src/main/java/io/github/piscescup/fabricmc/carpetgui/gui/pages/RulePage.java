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

import io.github.piscescup.fabricmc.carpetgui.api.DropdownOption;
import io.github.piscescup.fabricmc.carpetgui.api.RuleView;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A rule system's page: a mod or any other provider, identified without a mod loader dependency.
 */
public interface RulePage
    extends DropdownOption
{
    Component title();

    @Override
    default Component label() {
        return title();
    }

    List<? extends RuleView> rules();

    default boolean isVanilla() {
        return false;
    }

    default Component categoryLabel(String category) {
        return Component.translatableWithFallback("carpet-gui.category." + category, category);
    }
}

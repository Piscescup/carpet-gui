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

package io.github.piscescup.fabricmc.carpetgui.api;

import net.minecraft.network.chat.Component;

/**
 * Describes one selectable entry in a Carpet GUI drop-down list.
 * An option may represent a mod, a rule group, or another navigation target; its identifier
 * therefore must be stable and unique within the containing list.
 *
 * <pre>{@code
 * DropdownOption option = ...;
 * String persistedSelection = option.id();
 * Component visibleText = option.label();
 * }</pre>
 *
 * @see RuleSource
 * @see net.minecraft.network.chat.Component
 */
public interface DropdownOption {
    /**
     * Returns the stable machine-readable identifier of this option.
     *
     * <pre>{@code
     * if (option.id().equals(savedId)) {
     *     select(option);
     * }
     * }</pre>
     *
     * @return the non-changing identifier used for selection and persistence
     */
    String id();

    /**
     * Returns the text displayed for this option.
     *
     * <pre>{@code
     * drawLabel(option.label());
     * }</pre>
     *
     * @return the localized or literal display component
     */
    Component label();
}

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

import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleEditResult;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Optional persistence capability; ordinary edits and declared defaults remain separate.
 */
public interface PersistentRuleEditor extends RuleEditor {
    boolean canSaveDefault();

    /**
     * Whether this value has been acknowledged as a saved default by the backend.
     */
    default boolean isSavedDefault(String value) {
        return false;
    }

    Component defaultDisabledReason();

    RuleEditResult saveDefault(String value);

    default RuleEditResult saveDefault(String value, Consumer<RuleEditResult> completed) {
        return saveDefault(value);
    }
}

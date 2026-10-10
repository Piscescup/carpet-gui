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

package io.github.piscescup.fabricmc.carpetgui.gui.model;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * Backend controls editability, input shape and submission; GUI never writes raw rule fields.
 */
public interface RuleEditor {
    enum InputKind {
        BOOLEAN,
        NUMBER,
        TEXT
    }

    InputKind inputKind();

    List<String> suggestions();

    boolean strict();

    boolean editable();

    Component disabledReason();

    RuleEditResult submit(String value);

    /**
     * Optional asynchronous acknowledgement; legacy backends still return their immediate result.
     */
    default RuleEditResult submit(String value, Consumer<RuleEditResult> completed) {
        return submit(value);
    }
}

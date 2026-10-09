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

/** Queued is explicitly not an acknowledgement that the server accepted the change. */
public record RuleEditResult(boolean accepted, boolean queued, Component message) {
    public static RuleEditResult queuedRequest() {
        return new RuleEditResult(true, true, Component.translatable("carpet-gui.edit.queued"));
    }
    public static RuleEditResult queuedRequest(Component message) { return new RuleEditResult(true, true, message); }
    public static RuleEditResult applied() {
        return new RuleEditResult(true, false, Component.translatable("carpet-gui.edit.applied"));
    }
    public static RuleEditResult rejected(Component message) { return new RuleEditResult(false, false, message); }
}

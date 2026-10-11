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

package io.github.piscescup.fabricmc.carpetgui.store;

/** A new token for every join/disconnect, including reconnects to the same environment. */
public final class WorkspaceSession {
    private static volatile Token current = new Token(0, WorkspaceContext.offline());
    private WorkspaceSession() {}

    public static Token current() { return current; }

    public static void begin(WorkspaceContext context) {
        current = new Token(Math.incrementExact(current.generation()), context);
    }

    public record Token(long generation, WorkspaceContext context) {
        public boolean currentSession() { return equals(WorkspaceSession.current()); }
        public boolean canApply() { return currentSession() && context.connected(); }
    }
}

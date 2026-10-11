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

package io.github.piscescup.fabricmc.carpetgui.client;

import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleGroupPushController;
import io.github.piscescup.fabricmc.carpetgui.store.RuleGroupStore;
import io.github.piscescup.fabricmc.carpetgui.store.WorkspaceContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.world.level.storage.LevelResource;

import static io.github.piscescup.fabricmc.carpetgui.References.LOGGER;

/** Resolves identities on connection events, not every tick or rule render. */
final class ClientWorkspaceContext {
    private ClientWorkspaceContext() {}

    static void joined(Minecraft client) {
        WorkspaceContext context = WorkspaceContext.offline();
        try {
            var integrated = client.getSingleplayerServer();
            if (integrated != null) context = WorkspaceContext.world(integrated.getWorldPath(LevelResource.ROOT));
            else if (client.getCurrentServer() != null) {
                var address = ServerAddress.parseString(client.getCurrentServer().ip);
                context = WorkspaceContext.server(address.getHost(), address.getPort());
            }
        } catch (RuntimeException failure) {
            LOGGER.warn("Cannot identify Carpet GUI workspace; disabling group application for this session", failure);
        }
        switchTo(context);
    }

    static void disconnected() { switchTo(WorkspaceContext.offline()); }

    private static void switchTo(WorkspaceContext context) {
        RuleGroupPushController.disconnect();
        RuleGroupStore.switchContext(context);
    }
}

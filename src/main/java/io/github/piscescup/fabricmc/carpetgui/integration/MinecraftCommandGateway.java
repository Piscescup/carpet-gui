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

package io.github.piscescup.fabricmc.carpetgui.integration;

import carpet.network.CarpetClient;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleEditResult;
import io.github.piscescup.fabricmc.carpetgui.network.ClientRuleNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

public final class MinecraftCommandGateway implements RuleCommandGateway {
    private final Minecraft client;
    public MinecraftCommandGateway(Minecraft client) { this.client = client; }
    @Override public boolean carpetServer() {
        return client.getConnection() != null && client.player != null
                && (client.getSingleplayerServer() != null || CarpetClient.isCarpet());
    }
    @Override public boolean canExecute(String root) {
        return client.getConnection() != null && client.player != null
                && client.getConnection().getCommands().getRoot().getChild(root) != null;
    }
    @Override public RuleEditResult send(String command) {
        String root = command.split(" ", 2)[0];
        if (!canExecute(root)) return RuleEditResult.rejected(Component.translatable("carpet-gui.edit.no_permission"));
        client.getConnection().sendCommand(command);
        return RuleEditResult.queuedRequest();
    }
    @Override public RuleEditResult editCarpetRule(String manager, String rule, String value, boolean saveDefault,
                                                 Consumer<RuleEditResult> completed) {
        if (!canExecute(manager)) return RuleEditResult.rejected(Component.translatable("carpet-gui.edit.no_permission"));
        if (ClientRuleNetworking.supported()) return ClientRuleNetworking.send(manager, rule, value, saveDefault, completed);
        return RuleCommandGateway.super.editCarpetRule(manager, rule, value, saveDefault, completed);
    }
}

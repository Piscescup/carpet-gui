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

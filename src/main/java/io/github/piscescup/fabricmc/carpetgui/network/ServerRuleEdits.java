package io.github.piscescup.fabricmc.carpetgui.network;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.api.settings.InvalidRuleValueException;
import carpet.api.settings.RuleHelper;
import carpet.api.settings.SettingsManager;
import carpet.utils.CommandHelper;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static io.github.piscescup.fabricmc.carpetgui.References.LOGGER;

/**
 * Runs only on the server thread and uses the actual requesting player's source, never a console source.
 */
public final class ServerRuleEdits {
    private ServerRuleEdits() {
    }

    public static RuleEditResponse execute(RuleEditRequest request, CommandSourceStack playerSource) {
        if (!request.valid()) return fail(request, "carpet-gui.network.bad_request");
        SettingsManager manager = findManager(request.managerId());
        if (manager == null) return fail(request, "carpet-gui.network.unknown_manager");
        if (manager.locked()) return fail(request, "carpet-gui.edit.locked");
        var root = playerSource.dispatcher()
            .getRoot()
            .getChild(manager.identifier());
        if (!CommandHelper.canUseCommand(playerSource, CarpetSettings.carpetCommandPermissionLevel)
            || root == null || !root.canUse(playerSource)) {
            return fail(request, "carpet-gui.edit.no_permission");
        }
        var rule = manager.getCarpetRule(request.ruleId());
        if (rule == null) return fail(request, "carpet-gui.network.unknown_rule");
        try {
            var source = playerSource.withSuppressedOutput();
            if (request.operation() == RuleEditRequest.SET_VALUE) {
                rule.set(source, request.value());
            } else {
                // Carpet's persistence API is private. Keep its validation/config semantics via its registered command.
                int result = source.dispatcher()
                    .execute(manager.identifier() + " setDefault " + rule.name() + " " + request.value(), source);
                if (result <= 0) return fail(request, "carpet-gui.edit.invalid_value");
                // Carpet logs IO failures instead of throwing; verify before claiming that a default was saved.
                var config = source.getServer()
                    .getWorldPath(LevelResource.ROOT)
                    .resolve(manager.identifier() + ".conf");
                if (!Files.readAllLines(config)
                    .contains(rule.name() + " " + request.value())) {
                    return RuleEditResponse.result(request, false, RuleHelper.toRuleString(rule.value()), "carpet-gui.network.save_failed");
                }
            }
            return RuleEditResponse.result(
                request, true, RuleHelper.toRuleString(rule.value()),
                request.operation() == RuleEditRequest.SAVE_DEFAULT ? "carpet-gui.network.saved" : "carpet-gui.network.applied"
            );
        } catch (InvalidRuleValueException | CommandSyntaxException exception) {
            return fail(request, "carpet-gui.edit.invalid_value");
        } catch (IOException exception) {
            LOGGER.warn("Could not verify default for {}/{}", request.managerId(), request.ruleId(), exception);
            return RuleEditResponse.result(request, false, RuleHelper.toRuleString(rule.value()), "carpet-gui.network.save_failed");
        } catch (RuntimeException exception) {
            LOGGER.error("Rule request failed for {}/{}", request.managerId(), request.ruleId(), exception);
            return fail(request, "carpet-gui.network.server_error");
        }
    }

    public static SettingsManager findManager(String id) {
        if (CarpetServer.settingsManager != null && CarpetServer.settingsManager.identifier()
            .equals(id)) {
            return CarpetServer.settingsManager;
        }
        for (var extension : List.copyOf(CarpetServer.extensions)) {
            var manager = extension.extensionSettingsManager();
            if (manager != null && manager.identifier()
                .equals(id)) {
                return manager;
            }
        }
        return null;
    }

    private static RuleEditResponse fail(RuleEditRequest request, String key) {
        return RuleEditResponse.result(request, false, "", key);
    }
}

package io.github.piscescup.fabricmc.carpetgui.integration;

import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleEditResult;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

/**
 * Injectable transport/permission boundary for server-authoritative edits.
 */
public interface RuleCommandGateway {
    boolean carpetServer();

    boolean canExecute(String root);

    RuleEditResult send(String command);

    /**
     * Structured operation boundary. The default preserves command-only compatibility.
     */
    default RuleEditResult editCarpetRule(
        String manager, String rule, String value, boolean saveDefault,
        Consumer<RuleEditResult> completed
    ) {
        RuleEditResult result = send(manager + (saveDefault ? " setDefault " : " ") + rule + " " + value);
        return saveDefault && result.accepted() && result.queued()
            ? RuleEditResult.queuedRequest(Component.translatable("carpet-gui.edit.default_queued")) : result;
    }
}

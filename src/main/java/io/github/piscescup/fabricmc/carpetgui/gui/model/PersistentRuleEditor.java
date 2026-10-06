package io.github.piscescup.fabricmc.carpetgui.gui.model;

import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

/** Optional persistence capability; ordinary edits and declared defaults remain separate. */
public interface PersistentRuleEditor extends RuleEditor {
    boolean canSaveDefault();
    Component defaultDisabledReason();
    RuleEditResult saveDefault(String value);
    default RuleEditResult saveDefault(String value, Consumer<RuleEditResult> completed) { return saveDefault(value); }
}

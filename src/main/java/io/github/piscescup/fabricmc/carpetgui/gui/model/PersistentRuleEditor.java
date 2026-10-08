package io.github.piscescup.fabricmc.carpetgui.gui.model;

import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

/** Optional persistence capability; ordinary edits and declared defaults remain separate. */
public interface PersistentRuleEditor extends RuleEditor {
    boolean canSaveDefault();
    /** Whether this value has been acknowledged as a saved default by the backend. */
    default boolean isSavedDefault(String value) { return false; }
    Component defaultDisabledReason();
    RuleEditResult saveDefault(String value);
    default RuleEditResult saveDefault(String value, Consumer<RuleEditResult> completed) { return saveDefault(value); }
}

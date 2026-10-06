package io.github.piscescup.fabricmc.carpetgui.gui.model;

import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.function.Consumer;

/** Backend controls editability, input shape and submission; GUI never writes raw rule fields. */
public interface RuleEditor {
    enum InputKind { BOOLEAN, NUMBER, TEXT }
    InputKind inputKind();
    List<String> suggestions();
    boolean strict();
    boolean editable();
    Component disabledReason();
    RuleEditResult submit(String value);
    /** Optional asynchronous acknowledgement; legacy backends still return their immediate result. */
    default RuleEditResult submit(String value, Consumer<RuleEditResult> completed) { return submit(value); }
}

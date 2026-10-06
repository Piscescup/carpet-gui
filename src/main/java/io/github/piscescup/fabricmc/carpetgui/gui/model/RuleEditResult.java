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

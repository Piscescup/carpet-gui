package io.github.piscescup.fabricmc.carpetgui.gui.model;

/** Optional write capability, kept separate from the read-only RuleView contract. */
public interface EditableRuleView extends RuleView {
    RuleEditor editor();
}

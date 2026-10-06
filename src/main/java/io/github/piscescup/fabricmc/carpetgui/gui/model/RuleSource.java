package io.github.piscescup.fabricmc.carpetgui.gui.model;

import java.util.List;
import net.minecraft.network.chat.Component;

/**
 * Injectable source for demo data now, or an API adapter later.
 */
public interface RuleSource {
    List<? extends RulePage> pages();

    List<? extends RuleView> vanillaRules();

    default void refresh() {}
    default long revision() { return 0; }
    default Component notice(boolean vanilla) { return Component.empty(); }
}

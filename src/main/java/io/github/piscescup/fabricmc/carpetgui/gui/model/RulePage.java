package io.github.piscescup.fabricmc.carpetgui.gui.model;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A rule system's page: a mod or any other provider, identified without a mod loader dependency.
 */
public interface RulePage
    extends DropdownOption
{
    Component title();

    @Override
    default Component label() {
        return title();
    }

    List<? extends RuleView> rules();

    default boolean isVanilla() { return false; }

    default Component categoryLabel(String category) {
        return Component.translatableWithFallback("carpet-gui.category." + category, category);
    }
}

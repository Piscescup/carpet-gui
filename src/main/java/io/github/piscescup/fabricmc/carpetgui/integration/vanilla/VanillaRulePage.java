package io.github.piscescup.fabricmc.carpetgui.integration.vanilla;

import io.github.piscescup.fabricmc.carpetgui.gui.model.RulePage;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleView;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Supplier;

/**
 * Minecraft gamerules are a separate source tab, not a filter on every Carpet mod.
 */
public final class VanillaRulePage
    implements RulePage
{
    private final Supplier<List<? extends RuleView>> rules;

    public VanillaRulePage(Supplier<List<? extends RuleView>> rules) {
        this.rules = rules;
    }

    @Override
    public String id() {
        return "minecraft";
    }

    @Override
    public Component title() {
        return Component.literal("Minecraft");
    }

    @Override
    public List<? extends RuleView> rules() {
        return rules.get();
    }

    @Override
    public boolean isVanilla() {
        return true;
    }

    @Override
    public Component categoryLabel(String category) {
        return Component.translatableWithFallback("gamerule.category.minecraft." + category, category);
    }
}

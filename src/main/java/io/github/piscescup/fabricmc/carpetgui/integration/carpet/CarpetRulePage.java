package io.github.piscescup.fabricmc.carpetgui.integration.carpet;

import carpet.api.settings.CarpetRule;
import carpet.utils.Translations;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RulePage;
import io.github.piscescup.fabricmc.carpetgui.integration.RuleCommandGateway;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;

public final class CarpetRulePage implements RulePage {
    private final CarpetModBinding binding;
    private final List<CarpetRuleView> rules;
    private final CarpetClientTranslationResolver translations;

    public CarpetRulePage(
        CarpetModBinding binding, RuleCommandGateway gateway, CarpetClientTranslationResolver translations
    ) {
        this.binding = binding;
        this.translations = translations;
        rules = binding.rules()
            .stream()
            .sorted(Comparator.comparing(CarpetRule::name))
            .map(rule -> new CarpetRuleView(id(), rule.settingsManager(), rule, gateway, translations))
            .toList();
    }

    @Override
    public String id() {
        return binding.info()
            .carpetModId();
    }

    @Override
    public Component title() {
        return Component.literal(binding.info()
            .carpetFancyName());
    }

    @Override
    public List<CarpetRuleView> rules() {
        return rules;
    }

    @Override
    public Component categoryLabel(String category) {
        // A page may combine real managers. Look for the first category translation belonging to its rules.
        for (CarpetRule<?> rule : binding.rules()) {
            if (!rule.categories()
                .contains(category)) {
                continue;
            }
            String key = rule.settingsManager()
                .identifier() + ".category." + category;
            if (translations.has(id(), key)) return translations.resolve(id(), key, category);
        }
        String key = binding.rules()
            .isEmpty() ? category
            : binding.rules()
            .getFirst()
            .settingsManager()
            .identifier() + ".category." + category;
        return translations.resolve(id(), key, Translations.tr(key, category));
    }
}

/*
 * This file is part of the Carpet GUI project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  Fallen_Breath and contributors
 *
 * Carpet GUI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Carpet GUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Carpet GUI.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.piscescup.fabricmc.carpetgui.gui.pages;

import carpet.api.settings.CarpetRule;
import carpet.utils.Translations;
import io.github.piscescup.fabricmc.carpetgui.integration.RuleCommandGateway;
import io.github.piscescup.fabricmc.carpetgui.integration.carpet.CarpetClientTranslationResolver;
import io.github.piscescup.fabricmc.carpetgui.store.CarpetModBinding;
import io.github.piscescup.fabricmc.carpetgui.integration.carpet.CarpetRuleView;
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

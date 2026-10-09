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

package io.github.piscescup.fabricmc.carpetgui.integration.carpet;

import io.github.piscescup.fabricmc.carpetgui.gui.model.*;
import io.github.piscescup.fabricmc.carpetgui.integration.MinecraftCommandGateway;
import io.github.piscescup.fabricmc.carpetgui.integration.vanilla.VanillaRuleStore;
import io.github.piscescup.fabricmc.carpetgui.integration.vanilla.VanillaRuleView;
import io.github.piscescup.fabricmc.carpetgui.integration.vanilla.VanillaRulePage;
import io.github.piscescup.fabricmc.carpetgui.network.ClientRuleNetworking;
import io.github.piscescup.fabricmc.carpetgui.network.ClientRuleConfigurations;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class CarpetRuleSource
    implements RuleSource
{
    public static final AtomicLong RULE_REVISION = new AtomicLong();
    private final Minecraft client;
    private final MinecraftCommandGateway gateway;
    private final List<RulePage> pages;

    public CarpetRuleSource() {
        client = Minecraft.getInstance();
        gateway = new MinecraftCommandGateway(client);
        var translations = CarpetTranslationRegistry.discover();
        var resolver = new CarpetClientTranslationResolver(translations);
        List<RulePage> available = new ArrayList<>();
        for (CarpetModBinding binding : CarpetModRegistry.discover(translations)) {
            available.add(new CarpetRulePage(binding, gateway, resolver));
        }
        //#if MC >= 260000
        available.add(new VanillaRulePage(this::vanillaRules));
        //#endif
        pages = List.copyOf(available);
    }

    @Override
    public List<RulePage> pages() {
        return pages;
    }

    /** Mod IDs select pages; old independent-manager IDs remain usable as a compatibility alias. */
    public String resolvePageId(String requestedId) {
        if (pages.stream().anyMatch(page -> page.id().equals(requestedId))) return requestedId;
        for (CarpetManagerBinding binding : CarpetManagerBinding.discover()) {
            if (binding.manager().identifier().equals(requestedId)) return binding.modId();
        }
        return requestedId;
    }

    @Override
    public List<? extends RuleView> vanillaRules() {
        return VanillaRuleStore.values()
            .keySet()
            .stream()
            .filter(key -> BuiltInRegistries.GAME_RULE.getValue(key) != null)
            .map(key -> new VanillaRuleView(key, BuiltInRegistries.GAME_RULE.getValue(key), gateway))
            .sorted(Comparator.comparing(VanillaRuleView::id))
            .toList();
    }

    @Override
    public void refresh() {
        VanillaRuleStore.request(client);
    }

    @Override
    public long revision() {
        return RULE_REVISION.get() + VanillaRuleStore.revision() + ClientRuleConfigurations.revision();
    }

    @Override
    public Component notice(boolean vanilla) {
        if (vanilla) {
            if (client.getConnection() == null) return Component.translatable("carpet-gui.live.join_world");
            if (!VanillaRuleStore.permitted(client)) return Component.translatable("carpet-gui.live.vanilla_permission");
            if (!VanillaRuleStore.received()) return Component.translatable("carpet-gui.live.loading");
            return Component.translatable("carpet-gui.live.vanilla");
        }
        return Component.translatable(gateway.carpetServer()
            ? (ClientRuleNetworking.supported() ? "carpet-gui.live.packet" : "carpet-gui.live.carpet") : "carpet-gui.live.local");
    }
}

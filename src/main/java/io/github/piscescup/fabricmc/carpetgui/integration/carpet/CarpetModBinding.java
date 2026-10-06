package io.github.piscescup.fabricmc.carpetgui.integration.carpet;

import carpet.api.settings.CarpetRule;
import io.github.piscescup.fabricmc.carpetgui.api.CarpetModInfoApi;

import java.util.List;

/** A mod page can contain rules from one or more real SettingsManagers. */
public record CarpetModBinding(CarpetModInfoApi info, List<CarpetRule<?>> rules) {
    public CarpetModBinding {
        rules = List.copyOf(rules);
    }
}

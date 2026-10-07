package io.github.piscescup.fabricmc.carpetgui.api;

import carpet.api.settings.CarpetRule;

import java.util.Collection;

/** Optional rule ownership capability, especially for addons sharing another mod's manager. */
public interface CarpetModRulesApi extends CarpetModInfoApi {

    Collection<CarpetRule<?>> asCarpetRules();

}

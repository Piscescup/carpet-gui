package io.github.piscescup.fabricmc.carpetgui.api;

import carpet.api.settings.CarpetRule;

import java.util.Collection;

/** Optional rule ownership capability, especially for addons sharing another mod's manager. */
public interface CarpetModRulesApi extends CarpetModInfoApi, CarpetModTranslationApi {
    /** Already registered, live Carpet rules owned by this mod, not copies or newly created managers. */
    Collection<CarpetRule<?>> getRules();

    @Override
    default String getModId() {
        return carpetModId();
    }
}

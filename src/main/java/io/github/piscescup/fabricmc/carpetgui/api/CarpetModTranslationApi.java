package io.github.piscescup.fabricmc.carpetgui.api;

import io.github.piscescup.fabricmc.carpetgui.integration.carpet.CarpetTranslationResources;

import java.util.Map;
import java.util.Set;

/**
 * Common-side translation integration for a Carpet mod or addon.
 * Register in Fabric's {@value #ENTRYPOINT} entrypoint, or implement on a registered CarpetExtension.
 * Each addon supplies its own Fabric mod ID; it does not inherit the base mod's ID.
 */
@FunctionalInterface
public interface CarpetModTranslationApi {
    String ENTRYPOINT = "carpet-gui-translations";

    /** Actual Fabric metadata ID, not a command root or resource namespace. */
    String getModId();

    /**
     * Optional explicit ownership for managers whose owner cannot be found automatically.
     * Values must match SettingsManager.identifier(); empty keeps automatic discovery.
     */
    default Set<String> getSettingsManagerIds() {
        return Set.of();
    }

    /**
     * Optional translations in Carpet's original key format: manager.rule.ruleId.desc,
     * manager.rule.ruleId.extra.N and manager.category.categoryId.
     * By default, reads this mod's own {@code assets/<namespace>/lang} resources at runtime.
     * Override for a custom resource layout or to delegate to CarpetExtension.canHasTranslations.
     * Nothing is copied into Carpet GUI's language files. Implementations must not change global Carpet language.
     */
    default Map<String, String> getTranslations(String language) {
        return CarpetTranslationResources.read(getModId(), language);
    }
}

package io.github.piscescup.fabricmc.carpetgui.api;

import io.github.piscescup.fabricmc.carpetgui.integration.carpet.CarpetTranslationResources;

import java.util.Map;
import java.util.Set;

/** Identity of a Carpet mod page. Register under {@value #ENTRYPOINT}, or implement on a registered CarpetExtension. */
public interface CarpetModInfoApi {
    String ENTRYPOINT = "carpet-gui-mods";

    /** Actual Fabric mod ID; it is not necessarily the rule manager's command root. */
    String carpetModId();

    /** Displayed verbatim on the first-row tab, e.g. Carpet Mod or Carpet Igny Addition. */
    String carpetFancyName();

    default String getModId() {
        return carpetModId();
    }

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

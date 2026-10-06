package io.github.piscescup.fabricmc.carpetgui.api;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Implement this in a client mod's own "modmenu" entrypoint to open its corresponding Carpet rule page.
 * Only load implementations when Mod Menu is present; use {@link CarpetGuiScreens} for standalone entrypoints.
 */
@FunctionalInterface
public interface CarpetModMenuApi extends ModMenuApi {
    /** Returns SettingsManager.identifier(), which may differ from the Fabric mod ID. */
    String getCarpetSettingsManagerId();

    @Override
    default ConfigScreenFactory<?> getModConfigScreenFactory() {
        String pageId = this instanceof CarpetModInfoApi info ? info.carpetModId() : getCarpetSettingsManagerId();
        return parent -> CarpetGuiScreens.create(parent, pageId);
    }
}

package io.github.piscescup.fabricmc.carpetgui.integration.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import io.github.piscescup.fabricmc.carpetgui.api.CarpetModMenuApi;
import io.github.piscescup.fabricmc.carpetgui.api.CarpetGuiScreens;
import io.github.piscescup.fabricmc.carpetgui.api.CarpetModInfoApi;
import io.github.piscescup.fabricmc.carpetgui.integration.carpet.CarpetModRegistry;
import net.fabricmc.loader.api.FabricLoader;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mod Menu gives a mod's own factory priority over these automatic fallback factories.
 */
public final class CarpetGuiModMenu
    implements CarpetModMenuApi
{
    @Override
    public String getCarpetSettingsManagerId() {
        return "carpet";
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return CarpetGuiScreens::create;
    }

    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        Map<String, ConfigScreenFactory<?>> factories = new LinkedHashMap<>();
        factories.put("carpet", parent -> CarpetGuiScreens.create(parent, "carpet"));
        for (CarpetModInfoApi info : CarpetModRegistry.discoverInfos()) {
            if (FabricLoader.getInstance()
                .isModLoaded(info.carpetModId())) {
                factories.putIfAbsent(
                    info.carpetModId(),
                    parent -> CarpetGuiScreens.create(parent, info.carpetModId())
                );
            }
        }
        return Map.copyOf(factories);
    }
}

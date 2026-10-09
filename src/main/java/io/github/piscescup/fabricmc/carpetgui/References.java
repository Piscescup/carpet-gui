package io.github.piscescup.fabricmc.carpetgui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.piscescup.fabricmc.carpetgui.adapter.PackageRef;
import io.github.piscescup.fabricmc.carpetgui.adapter.CarpetAddonAdapter;
import io.github.piscescup.fabricmc.carpetgui.adapter.codec.CarpetAddonAdapterJsonCodec;
import io.github.piscescup.fabricmc.carpetgui.adapter.Accessor;
import io.github.piscescup.fabricmc.carpetgui.adapter.codec.AccessorJsonCodec;
import io.github.piscescup.fabricmc.carpetgui.adapter.codec.PackageRefJsonCodec;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author REN YuanTong
 * @since
 */
public class References {

    public static final String MOD_ID = "carpet-gui";

    public static final FabricLoader FABRIC_LOADER = FabricLoader.getInstance();

    public static final ModContainer CARPET_GUI_MOD_CONTAINER = FABRIC_LOADER
        .getModContainer(MOD_ID)
        .orElseThrow();

    public static final String MOD_NAME = CARPET_GUI_MOD_CONTAINER
        .getMetadata()
        .getName();

    public static final String MOD_VERSION = CARPET_GUI_MOD_CONTAINER
        .getMetadata()
        .getVersion()
        .getFriendlyString();

    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    public static final Gson GSON = new GsonBuilder()
        .registerTypeHierarchyAdapter(PackageRef.class, new PackageRefJsonCodec())
        .registerTypeHierarchyAdapter(Accessor.class, new AccessorJsonCodec())
        .registerTypeHierarchyAdapter(CarpetAddonAdapter.class, new CarpetAddonAdapterJsonCodec())
        .setPrettyPrinting()
        .create();

    public static final boolean DEBUG = LOGGER.isDebugEnabled();

    public static Identifier fromPath(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}

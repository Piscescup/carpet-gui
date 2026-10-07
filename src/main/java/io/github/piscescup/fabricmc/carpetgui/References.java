package io.github.piscescup.fabricmc.carpetgui;

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

    public static final String MOD_NAME = "Carpet GUI";

    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    public static final boolean DEBUG = LOGGER.isDebugEnabled();

    public static Identifier fromPath(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}

package io.github.piscescup.fabricmc.carpetgui.api;

import io.github.piscescup.fabricmc.carpetgui.gui.workspace.CarpetWorkspaceScreen;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleSource;
import io.github.piscescup.fabricmc.carpetgui.integration.carpet.CarpetRuleSource;
import net.minecraft.client.gui.screens.Screen;

/**
 * Public factories, usable without Mod Menu. Prefer the Fabric mod ID for the selected page.
 */
public final class CarpetGuiScreens {
    private CarpetGuiScreens() {
    }

    public static Screen create(Screen parent) {
        return new CarpetWorkspaceScreen(parent, new CarpetRuleSource(), null).getAsScreen();
    }

    public static Screen create(Screen parent, String modId) {
        CarpetRuleSource source = new CarpetRuleSource();
        return new CarpetWorkspaceScreen(parent, source, source.resolvePageId(modId)).getAsScreen();
    }

    /** Creates the same workspace for a custom rule system; null initialPageId opens Home. */
    public static Screen create(Screen parent, RuleSource source, String initialPageId) {
        return new CarpetWorkspaceScreen(parent, source, initialPageId).getAsScreen();
    }
}

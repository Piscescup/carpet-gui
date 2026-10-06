package io.github.piscescup.fabricmc.carpetgui.api;

/** Identity of a Carpet mod page. Register under {@value #ENTRYPOINT}, or implement on a registered CarpetExtension. */
public interface CarpetModInfoApi {
    String ENTRYPOINT = "carpet-gui-mods";

    /** Actual Fabric mod ID; it is not necessarily the rule manager's command root. */
    String carpetModId();

    /** Displayed verbatim on the first-row tab, e.g. Carpet Mod or Carpet Igny Addition. */
    String carpetFancyName();
}

package io.github.piscescup.fabricmc.carpetgui.gui.model;

import net.minecraft.network.chat.Component;

/**
 * A selectable GUI entry, not necessarily a mod. IDs must be stable and unique.
 */
public interface DropdownOption {
    String id();

    Component label();
}

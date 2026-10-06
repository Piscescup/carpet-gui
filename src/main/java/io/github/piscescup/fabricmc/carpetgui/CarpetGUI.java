package io.github.piscescup.fabricmc.carpetgui;

import net.fabricmc.api.ModInitializer;

import io.github.piscescup.fabricmc.carpetgui.network.RuleNetworking;


public class CarpetGUI implements ModInitializer {

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		RuleNetworking.initialize();
	}
}

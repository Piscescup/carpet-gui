package io.github.piscescup.fabricmc.carpetgui;

import io.github.piscescup.fabricmc.carpetgui.datagen.ENUSLanguageProvider;
import io.github.piscescup.fabricmc.carpetgui.datagen.ZHCNLanguageProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class CarpetGUIDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		var pack = fabricDataGenerator.createPack();
		pack.addProvider(ENUSLanguageProvider::new);
		pack.addProvider(ZHCNLanguageProvider::new);
	}
}

package io.github.piscescup.fabricmc.carpetgui.adapter;


/**
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public interface AdapterBuilder<AB extends AdapterBuilder<AB>> {
    AB carpetExtensionClassName(PackageRef packageRef);

    AB settingsClassName(PackageRef packageRef);

    default AB carpetExtensionClassName(String carpetExtensionClassName) {
        return carpetExtensionClassName(PackageRef.relative(carpetExtensionClassName));
    }

    default AB settingsClassName(String settingsClassName) {
        return settingsClassName(PackageRef.relative(settingsClassName));
    }

    AB extensionAccessor(Accessor<?>  extensionAccessor);

    AB settingsAccessor(Accessor<?> settingsAccessor);

    CarpetAddonAdapter build();

}

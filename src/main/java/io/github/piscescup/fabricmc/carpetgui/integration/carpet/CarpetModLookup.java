package io.github.piscescup.fabricmc.carpetgui.integration.carpet;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.Collection;
import java.util.Optional;

/**
 * Resolves ownership by the extension class; command-root IDs are only a fallback.
 */
public final class CarpetModLookup {
    private CarpetModLookup() {
    }

    public static Optional<ModContainer> find(Class<?> owner, String managerId) {
        return find(
            FabricLoader.getInstance()
                .getAllMods(), owner.getName(), managerId
        );
    }

    public static Optional<ModContainer> find(Collection<ModContainer> mods, String ownerClassName, String managerId) {
        String classPath = ownerClassName.replace('.', '/') + ".class";
        Optional<ModContainer> owner = mods.stream()
            .filter(mod -> mod.findPath(classPath)
                .isPresent())
            .findFirst();
        return owner.isPresent() ? owner : mods.stream()
            .filter(mod -> mod.getMetadata()
                .getId()
                .equals(managerId))
            .findFirst();
    }
}

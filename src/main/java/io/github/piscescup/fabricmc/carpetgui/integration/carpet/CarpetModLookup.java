package io.github.piscescup.fabricmc.carpetgui.integration.carpet;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.lang.reflect.Modifier;

/**
 * Resolves ownership by the extension class; command-root IDs are only a fallback.
 */
public final class CarpetModLookup {
    private CarpetModLookup() {
    }

    public static Optional<ModContainer> find(Class<?> owner, String managerId) {
        return find(
            FabricLoader.getInstance()
                .getAllMods(),
            owner, managerId
        );
    }

    /**
     * Class origin wins; public reflected Mod IDs help wrappers whose class path cannot be found.
     */
    public static Optional<ModContainer> find(Collection<ModContainer> mods, Class<?> owner, String managerId) {
        Optional<ModContainer> origin = findClassOwner(mods, owner.getName());
        if (origin.isPresent()) return origin;
        for (String methodName : java.util.List.of("getModId", "carpetModId")) {
            try {
                var method = owner.getMethod(methodName);
                if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != String.class) continue;
                Optional<ModContainer> mod = byId(mods, (String) method.invoke(null));
                if (mod.isPresent()) return mod;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                // Optional convention: keep checking origin/metadata fallbacks.
            }
        }
        for (String fieldName : java.util.List.of("MOD_ID", "MODID", "modId")) {
            try {
                var field = owner.getField(fieldName);
                if (!Modifier.isStatic(field.getModifiers()) || field.getType() != String.class) continue;
                Optional<ModContainer> mod = byId(mods, (String) field.get(null));
                if (mod.isPresent()) return mod;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                // No private-field access and no hard-coded addon package names.
            }
        }
        return byId(mods, managerId);
    }

    private static Optional<ModContainer> byId(Collection<ModContainer> mods, String id) {
        return mods.stream()
            .filter(mod -> mod.getMetadata()
                .getId()
                .equals(id))
            .findFirst();
    }

    /**
     * Optional public name conventions, falling back to the actual Fabric metadata name.
     */
    public static String displayName(Object extension, String fallback) {
        Class<?> type = extension.getClass();
        for (String name : List.of("carpetFancyName", "getFancyName", "getModName")) {
            try {
                var method = type.getMethod(name);
                if (method.getReturnType() != String.class) continue;
                String value = (String) method.invoke(Modifier.isStatic(method.getModifiers()) ? null : extension);
                if (value != null && !value.isBlank()) return value;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                // Optional conventions must not prevent metadata fallback.
            }
        }
        for (String name : List.of("fancyName", "FANCY_NAME", "MOD_NAME")) {
            try {
                var field = type.getField(name);
                if (field.getType() != String.class) continue;
                String value = (String) field.get(Modifier.isStatic(field.getModifiers()) ? null : extension);
                if (value != null && !value.isBlank()) return value;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                // Never access private fields.
            }
        }
        return fallback;
    }

    private static Optional<ModContainer> findClassOwner(Collection<ModContainer> mods, String ownerClassName) {
        String classPath = ownerClassName.replace('.', '/') + ".class";
        return mods.stream()
            .filter(mod -> mod.findPath(classPath)
                .isPresent())
            .findFirst();
    }

    public static Optional<ModContainer> find(Collection<ModContainer> mods, String ownerClassName, String managerId) {
        Optional<ModContainer> owner = findClassOwner(mods, ownerClassName);
        return owner.isPresent() ? owner : byId(mods, managerId);
    }
}

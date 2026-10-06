package io.github.piscescup.fabricmc.carpetgui.integration.carpet;

import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Regression checks runnable without starting Minecraft or installing an addon. */
public final class AddonDiscoveryTest {
    public static final class MethodId {
        public static String getModId() { return "addon"; }
    }
    public static final class FieldId {
        public static final String MOD_ID = "addon";
    }
    public static final class BadId {
        public static String getModId() { throw new IllegalStateException("not initialized"); }
        public static final String MOD_ID = "addon";
    }
    public static final class UnknownId {
        public static String getModId() { return "not-installed"; }
    }
    public static final class PrivateId {
        private static final String MOD_ID = "addon";
    }
    public static final class MethodName {
        public String getFancyName() { return "Runtime name"; }
        public static final String fancyName = "Field name";
    }
    public static final class FieldName {
        public String getFancyName() { throw new IllegalStateException("not ready"); }
        public static final String fancyName = "Field name";
    }
    public static final class EmptyName {
        public String getFancyName() { return " "; }
        private static final String fancyName = "Private";
    }

    private static ModContainer mod(String id, Class<?> owner) {
        ModMetadata metadata = (ModMetadata) Proxy.newProxyInstance(ModMetadata.class.getClassLoader(),
            new Class<?>[]{ModMetadata.class}, (proxy, method, args) -> {
                if (method.getName().equals("getId")) return id;
                if (method.getName().equals("getName")) return id;
                throw new UnsupportedOperationException(method.getName());
            });
        String classPath = owner == null ? "" : owner.getName().replace('.', '/') + ".class";
        return (ModContainer) Proxy.newProxyInstance(ModContainer.class.getClassLoader(),
            new Class<?>[]{ModContainer.class}, (proxy, method, args) -> {
                if (method.getName().equals("getMetadata")) return metadata;
                if (method.getName().equals("findPath")) {
                    return classPath.equals(args[0]) ? Optional.of(Path.of(classPath)) : Optional.empty();
                }
                throw new UnsupportedOperationException(method.getName());
            });
    }

    private static void equal(Object actual, Object expected) {
        if (!java.util.Objects.equals(actual, expected)) throw new AssertionError(actual + " != " + expected);
    }

    private static String owner(List<ModContainer> mods, Class<?> type, String manager) {
        return CarpetModLookup.find(mods, type, manager).map(mod -> mod.getMetadata().getId()).orElse("");
    }

    public static void main(String[] args) {
        var mods = List.of(mod("carpet", null), mod("addon", null));
        equal(owner(mods, MethodId.class, "carpet"), "addon");
        equal(owner(mods, FieldId.class, "carpet"), "addon");
        equal(owner(mods, BadId.class, ""), "addon");
        equal(owner(mods, UnknownId.class, ""), "");
        equal(owner(mods, PrivateId.class, ""), "");
        equal(owner(mods, Object.class, "carpet"), "carpet");
        // Origin must win over a stale reflected ID and a shared parent command root.
        equal(owner(List.of(mod("origin", MethodId.class), mod("addon", null), mod("carpet", null)),
            MethodId.class, "carpet"), "origin");
        equal(ReflectiveAddonAdapters.displayName(new MethodName(), "Metadata"), "Runtime name");
        equal(ReflectiveAddonAdapters.displayName(new FieldName(), "Metadata"), "Field name");
        equal(ReflectiveAddonAdapters.displayName(new EmptyName(), "Metadata"), "Metadata");
        equal(ReflectiveAddonAdapters.displayName(new Object(), "Metadata"), "Metadata");
        System.out.println("Addon discovery: 11 checks passed");
    }
}

package io.github.piscescup.fabricmc.carpetgui.adapter;

/** A class name with an explicit relative or absolute resolution mode. */
public interface PackageRef {
    String DOT = ".";

    String name();

    boolean absolute();

    default String toCanonicalPackage(String parentPackageName) {
        if (absolute()) return name();
        if (parentPackageName == null || parentPackageName.isBlank()) {
            throw new IllegalArgumentException("A parent package is required for relative class " + name());
        }
        return parentPackageName + DOT + name();
    }

    static PackageRef absolute(String className) {
        return new Named(className, true);
    }

    static PackageRef relative(String className) {
        return new Named(className, false);
    }

    record Named(String name, boolean absolute) implements PackageRef {
        public Named {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Class name must not be blank");
            }
        }
    }
}

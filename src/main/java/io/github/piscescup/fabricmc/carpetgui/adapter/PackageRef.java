/*
 * This file is part of the Carpet GUI project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  PiscesCup and contributors
 *
 * Carpet GUI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Carpet GUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Carpet GUI.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.piscescup.fabricmc.carpetgui.adapter;

/**
 * A class-name reference with an explicit absolute or parent-relative resolution mode.
 * Keeping the mode separate makes adapter configurations serializable without loading the class.
 *
 * <pre>{@code
 * PackageRef relative = PackageRef.relative("settings.MySettings");
 * String name = relative.toCanonicalPackage("dev.example");
 * // dev.example.settings.MySettings
 * }</pre>
 *
 * @see AdapterBuilder
 * @see io.github.piscescup.fabricmc.carpetgui.adapter.codec.PackageRefJsonCodec
 */
public interface PackageRef {
    /** Separator between Java package and class-name segments. */
    String DOT = ".";

    /**
     * Returns the stored class name without resolving it.
     *
     * @return the absolute or relative name supplied at construction
     */
    String name();

    /**
     * Indicates whether {@link #name()} is already fully qualified.
     *
     * @return {@code true} for an absolute class name
     */
    boolean absolute();

    /**
     * Resolves this reference to a fully qualified class name.
     *
     * <pre>{@code
     * String owner = PackageRef.relative("Rules").toCanonicalPackage("dev.example");
     * }</pre>
     *
     * @param parentPackageName package prepended to a relative reference; ignored for an absolute reference
     * @return the fully qualified class name
     * @throws IllegalArgumentException if this reference is relative and the parent package is null or blank
     */
    default String toCanonicalPackage(String parentPackageName) {
        if (absolute()) return name();
        if (parentPackageName == null || parentPackageName.isBlank()) {
            throw new IllegalArgumentException("A parent package is required for relative class " + name());
        }
        return parentPackageName + DOT + name();
    }

    /**
     * Creates an absolute class reference.
     *
     * <pre>{@code
     * PackageRef ref = PackageRef.absolute("dev.example.MySettings");
     * }</pre>
     *
     * @param className the fully qualified class name
     * @return an absolute reference
     * @throws IllegalArgumentException if {@code className} is {@code null} or blank
     */
    static PackageRef absolute(String className) {
        return new Named(className, true);
    }

    /**
     * Creates a class reference relative to an adapter's parent package.
     *
     * <pre>{@code
     * PackageRef ref = PackageRef.relative("settings.MySettings");
     * }</pre>
     *
     * @param className the parent-relative class name
     * @return a relative reference
     * @throws IllegalArgumentException if {@code className} is {@code null} or blank
     */
    static PackageRef relative(String className) {
        return new Named(className, false);
    }

    /**
     * Immutable named implementation used by the factory methods.
     *
     * @param name the stored class name
     * @param absolute whether the name is fully qualified
     */
    record Named(String name, boolean absolute) implements PackageRef {
        public Named {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Class name must not be blank");
            }
        }
    }
}

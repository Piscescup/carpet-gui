/*
 * This file is part of the Carpet GUI project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  Fallen_Breath and contributors
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

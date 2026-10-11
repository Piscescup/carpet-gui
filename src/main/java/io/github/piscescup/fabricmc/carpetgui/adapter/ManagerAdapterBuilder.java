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

/**
 * Builds a {@link ManagerCarpetAddonAdapter} that reads a dedicated {@code SettingsManager}
 * directly instead of discovering rules from addon-specific data structures.
 *
 * <pre>{@code
 * CarpetAddonAdapter adapter = CarpetAddonAdapter.managerBuilder("my_mod", "dev.example")
 *     .managerClassName("MyExtension")
 *     .managerAccessor(Accessor.ofStaticField("MANAGER"))
 *     .build();
 * }</pre>
 *
 * @see ManagerCarpetAddonAdapter
 * @see CarpetAddonAdapter#managerBuilder(String, String)
 */
public interface ManagerAdapterBuilder extends AdapterBuilder<ManagerAdapterBuilder> {
    /**
     * Configures the class declaring the manager member; it does not have to implement
     * {@code CarpetExtension}.
     *
     * @param packageRef absolute or parent-relative declaring class reference
     * @return this builder
     * @throws NullPointerException if {@code packageRef} is {@code null}
     * @throws IllegalArgumentException if a relative reference has no parent package
     */
    ManagerAdapterBuilder managerClassName(PackageRef packageRef);

    /**
     * Configures a parent-relative manager declaring class name.
     *
     * <pre>{@code
     * builder.managerClassName("MyExtension");
     * }</pre>
     *
     * @param className class name relative to the builder's parent package
     * @return this builder
     * @throws IllegalArgumentException if the name is blank or no parent package is available
     */
    default ManagerAdapterBuilder managerClassName(String className) {
        return managerClassName(PackageRef.relative(className));
    }

    /**
     * Configures the accessor returning the dedicated manager. Static access targets the manager
     * class; instance access targets the live extension.
     *
     * <pre>{@code
     * builder.managerAccessor(Accessor.ofStaticField("MANAGER"));
     * }</pre>
     *
     * @param accessor accessor whose value must be a dedicated
     *                 {@link carpet.api.settings.SettingsManager}
     * @return this builder
     * @throws NullPointerException if {@code accessor} is {@code null}
     */
    ManagerAdapterBuilder managerAccessor(Accessor<?> accessor);
}

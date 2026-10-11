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
 * Common fluent configuration for reflection-based Carpet addon adapters.
 * Class references may be absolute or relative to the parent package passed to the factory that
 * created this builder.
 *
 * <pre>{@code
 * CarpetAddonAdapter adapter = CarpetAddonAdapter.annotationBuilder("my_mod", "dev.example")
 *     .carpetExtensionClassName("MyExtension")
 *     .settingsClassName("MySettings")
 *     .onCarpetRuleAnnotation()
 *     .build();
 * }</pre>
 *
 * @param <AB> the concrete builder type returned by fluent methods
 * @author REN YuanTong
 * @since 1.0.0
 * @see CarpetAddonAdapter
 * @see PackageRef
 * @see Accessor
 */
public interface AdapterBuilder<AB extends AdapterBuilder<AB>> {
    /**
     * Configures the Carpet extension class.
     *
     * @param packageRef absolute or parent-relative extension class reference
     * @return this builder
     * @throws NullPointerException if {@code packageRef} is {@code null}
     * @throws IllegalArgumentException if a relative reference has no parent package
     */
    AB carpetExtensionClassName(PackageRef packageRef);

    /**
     * Configures the class that owns or exposes the addon's settings.
     *
     * @param packageRef absolute or parent-relative settings class reference
     * @return this builder
     * @throws NullPointerException if {@code packageRef} is {@code null}
     * @throws IllegalArgumentException if a relative reference has no parent package
     */
    AB settingsClassName(PackageRef packageRef);

    /**
     * Configures a parent-relative Carpet extension class name.
     *
     * <pre>{@code
     * builder.carpetExtensionClassName("MyExtension");
     * }</pre>
     *
     * @param carpetExtensionClassName class name relative to the builder's parent package
     * @return this builder
     * @throws IllegalArgumentException if the name is blank or no parent package is available
     */
    default AB carpetExtensionClassName(String carpetExtensionClassName) {
        return carpetExtensionClassName(PackageRef.relative(carpetExtensionClassName));
    }

    /**
     * Configures a parent-relative settings class name.
     *
     * <pre>{@code
     * builder.settingsClassName("settings.MySettings");
     * }</pre>
     *
     * @param settingsClassName class name relative to the builder's parent package
     * @return this builder
     * @throws IllegalArgumentException if the name is blank or no parent package is available
     */
    default AB settingsClassName(String settingsClassName) {
        return settingsClassName(PackageRef.relative(settingsClassName));
    }

    /**
     * Configures how the live Carpet extension instance is obtained.
     *
     * <pre>{@code
     * builder.extensionAccessor(Accessor.ofRegisteredExtension());
     * }</pre>
     *
     * @param extensionAccessor accessor evaluated according to its static or instance mode
     * @return this builder
     */
    AB extensionAccessor(Accessor<?>  extensionAccessor);

    /**
     * Configures how a settings instance is read when another accessor requires instance access.
     *
     * <pre>{@code
     * builder.settingsAccessor(Accessor.ofInstanceMethod("settings"));
     * }</pre>
     *
     * @param settingsAccessor accessor evaluated against the settings class or live extension
     * @return this builder
     */
    AB settingsAccessor(Accessor<?> settingsAccessor);

    /**
     * Validates the configuration and creates an adapter.
     *
     * <pre>{@code
     * CarpetAddonAdapter adapter = builder.build();
     * }</pre>
     *
     * @return the configured addon adapter
     * @throws IllegalArgumentException if a required identifier or class name is blank
     * @throws NullPointerException if a required class reference or accessor is missing
     */
    CarpetAddonAdapter build();

}

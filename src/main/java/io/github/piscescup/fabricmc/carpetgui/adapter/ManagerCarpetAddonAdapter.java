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

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.SettingsManager;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Reads a dedicated {@link SettingsManager} without importing optional addon types and exposes all
 * rules registered in it. Static manager access requires only its declaring class; instance access
 * uses the configured live Carpet extension. Carpet's global manager is rejected because it cannot
 * establish ownership for a single addon.
 *
 * <pre>{@code
 * ManagerCarpetAddonAdapter adapter = CarpetAddonAdapter
 *     .managerBuilder("my_mod", "dev.example")
 *     .managerClassName("MyExtension")
 *     .managerAccessor(Accessor.ofStaticField("MANAGER"))
 *     .build();
 * }</pre>
 *
 * @see ManagerAdapterBuilder
 * @see SettingsManager
 * @see CarpetAddonAdapter
 */
public final class ManagerCarpetAddonAdapter extends CarpetAddonAdapter {
    private final PackageRef managerClassRef;
    private final String managerClassCanonicalName;
    private final Accessor<?> managerAccessor;

    ManagerCarpetAddonAdapter(ManagerBuilder builder) {
        super(builder);
        this.managerClassRef = builder.managerClassRef;
        this.managerClassCanonicalName = builder.managerClassCanonicalName;
        this.managerAccessor = builder.managerAccessor;
    }

    /**
     * Returns the unresolved class reference for the member that supplies the manager.
     *
     * @return the configured manager declaring-class reference
     */
    public PackageRef managerClassRef() { return managerClassRef; }

    /**
     * Returns the accessor used to obtain the dedicated manager.
     *
     * @return the configured manager accessor
     */
    public Accessor<?> managerAccessor() { return managerAccessor; }

    /**
     * Returns the names of every rule registered in the dedicated manager.
     *
     * @param args must be empty
     * @return the manager's rule names, or an empty collection while the manager is unavailable
     * @throws IllegalArgumentException if any arguments are supplied
     */
    @Override
    protected Collection<String> getRuleNames(Object... args) {
        if (args != null && args.length != 0) {
            throw new IllegalArgumentException("Manager adapter does not accept rule-name arguments");
        }
        SettingsManager manager = settingsManager();
        return manager == null ? List.of()
            : manager.getCarpetRules().stream()
                .map(CarpetRule::name)
                .toList();
    }

    /**
     * Re-reads the configured accessor on every call so an unavailable or replaced manager is not
     * cached. Lookup failures are logged and converted to {@code null}.
     *
     * @return the dedicated manager, or {@code null} while unavailable or on failure
     */
    @Override
    public SettingsManager settingsManager() {
        try {
            Class<?> owner = load(managerClassCanonicalName);
            Object target;
            if (managerAccessor.isStaticAccess()) {
                target = owner;
            } else {
                Object instance = extension();
                if (instance == null) return null;
                target = owner.cast(instance);
            }
            Object value = managerAccessor.get(target);
            if (value == null) return null;
            SettingsManager resolved = SettingsManager.class.cast(value);
            if (resolved == CarpetServer.settingsManager) {
                throw new IllegalArgumentException("A dedicated manager is required for addon " + modId);
            }
            return resolved;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            warn("settings manager", failure);
            return null;
        }
    }
}

/** Package-private implementation of {@link ManagerAdapterBuilder}. */
class ManagerBuilder extends Builder<ManagerAdapterBuilder> implements ManagerAdapterBuilder {
    PackageRef managerClassRef;
    String managerClassCanonicalName;
    Accessor<?> managerAccessor;

    ManagerBuilder(String modId, String packageName) {
        super(modId, packageName);
    }

    @Override
    public ManagerAdapterBuilder managerClassName(PackageRef packageRef) {
        this.managerClassRef = Objects.requireNonNull(packageRef, "Class reference");
        this.managerClassCanonicalName = Objects.requireNonNull(packageRef, "Manager class")
            .toCanonicalPackage(packageName);
        return this;
    }

    @Override
    public ManagerAdapterBuilder managerAccessor(Accessor<?> accessor) {
        this.managerAccessor = Objects.requireNonNull(accessor, "Manager accessor");
        return this;
    }

    @Override
    public ManagerCarpetAddonAdapter build() {
        requireText(modId, "Mod ID");
        requireText(managerClassCanonicalName, "Manager class");
        Objects.requireNonNull(managerAccessor, "Manager accessor");
        if (!managerAccessor.isStaticAccess()) {
            requireText(carpetExtensionClassCanonicalName, "Extension class for instance manager access");
        }
        if (carpetExtensionClassCanonicalName != null) {
            Objects.requireNonNull(extensionAccessor, "Extension accessor");
        }
        return new ManagerCarpetAddonAdapter(this);
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
    }
}

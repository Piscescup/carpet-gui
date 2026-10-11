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

package io.github.piscescup.fabricmc.carpetgui.store;

import java.io.IOException;
import java.net.IDN;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/** Client-local environment identity. Display names never determine storage paths. */
public record WorkspaceContext(String id, String label, Path worldDirectory) {
    public WorkspaceContext {
        if (id == null || !id.matches("offline|(?:server|world)-[0-9a-f]{64}") || label == null) {
            throw new IllegalArgumentException("Invalid workspace context");
        }
        if (id.startsWith("world-") != (worldDirectory != null)) throw new IllegalArgumentException("Invalid save directory");
    }

    public static WorkspaceContext offline() { return new WorkspaceContext("offline", "offline", null); }
    public boolean connected() { return !id.equals("offline"); }

    /** World-local state travels with the save; multiplayer state lives beside config. */
    public Path file(Path globalDirectory, Path multiplayerDirectory) {
        if (worldDirectory != null) return worldDirectory.resolve("carpet-gui").resolve("rule-groups.json");
        if (!connected()) return globalDirectory.resolve("offline").resolve("rule-groups.json");
        String safeAddress = label.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safeAddress.length() > 80) safeAddress = safeAddress.substring(0, 80);
        return multiplayerDirectory.resolve("Multiplayergames_" + safeAddress + "_" + id.substring("server-".length()))
            .resolve("rule-groups.json");
    }

    /** Uses the entered host/port, without DNS resolution or the server-list display name. */
    public static WorkspaceContext server(String host, int port) {
        String normalized = host.strip().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("[") && normalized.endsWith("]")) normalized = normalized.substring(1, normalized.length() - 1);
        if (normalized.endsWith(".")) normalized = normalized.substring(0, normalized.length() - 1);
        if (!normalized.contains(":")) normalized = IDN.toASCII(normalized).toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || port < 1 || port > 65535) throw new IllegalArgumentException("Invalid server address");
        String address = (normalized.contains(":") ? "[" + normalized + "]" : normalized) + ":" + port;
        return new WorkspaceContext("server-" + hash(address), address, null);
    }

    /** Canonical save root: renaming a world's display title or changing dimension keeps its state. */
    public static WorkspaceContext world(Path directory) {
        Path canonical = directory.toAbsolutePath().normalize();
        try { canonical = canonical.toRealPath(); } catch (IOException ignored) { /* Save may be closing. */ }
        return new WorkspaceContext("world-" + hash(canonical.toString()), canonical.getFileName().toString(), canonical);
    }

    private static String hash(String identity) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}

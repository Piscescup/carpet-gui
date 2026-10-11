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

package io.github.piscescup.fabricmc.carpetgui.gui.workspace;

import io.github.piscescup.fabricmc.carpetgui.store.RuleGroupStore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds the workspace-only directory view without changing persisted group names. */
final class RuleGroupTree {
    static final String ORIGIN = "origin";
    private static final Comparator<String> NAMES = String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder());

    private RuleGroupTree() {}

    static List<Folder> of(List<RuleGroupStore.Group> groups) {
        MutableFolder root = new MutableFolder(ORIGIN, ORIGIN, 0);
        for (RuleGroupStore.Group group : groups) {
            List<String> segments = segments(group.name());
            List<String> folders = segments.size() <= 1 ? List.of() : segments.subList(0, segments.size() - 1);
            String leafName = segments.isEmpty() ? group.id() : segments.getLast();
            MutableFolder current = root;
            for (String folderName : folders) {
                String path = current.path + "/" + folderName;
                int depth = current.depth + 1;
                current = current.children.computeIfAbsent(folderName, ignored -> new MutableFolder(path, folderName, depth));
            }
            current.groups.add(new Entry(group, leafName));
        }
        return List.of(freeze(root));
    }

    private static List<String> segments(String name) {
        List<String> result = new ArrayList<>();
        for (String segment : name.split("/")) {
            String clean = segment.strip();
            if (!clean.isEmpty()) result.add(clean);
        }
        return result;
    }

    private static List<Folder> freezeChildren(MutableFolder parent) {
        return parent.children.values().stream()
            .sorted(Comparator.comparing(folder -> folder.name, NAMES))
            .map(RuleGroupTree::freeze)
            .toList();
    }

    private static Folder freeze(MutableFolder folder) {
        return new Folder(
            folder.path,
            folder.name,
            folder.depth,
            freezeChildren(folder),
            folder.groups.stream().sorted(Comparator.comparing(Entry::name, NAMES)).toList()
        );
    }

    record Folder(String path, String name, int depth, List<Folder> children, List<Entry> groups) {}

    record Entry(RuleGroupStore.Group group, String name) {}

    private static final class MutableFolder {
        private final String path;
        private final String name;
        private final int depth;
        private final Map<String, MutableFolder> children = new LinkedHashMap<>();
        private final List<Entry> groups = new ArrayList<>();

        private MutableFolder(String path, String name, int depth) {
            this.path = path;
            this.name = name;
            this.depth = depth;
        }
    }
}

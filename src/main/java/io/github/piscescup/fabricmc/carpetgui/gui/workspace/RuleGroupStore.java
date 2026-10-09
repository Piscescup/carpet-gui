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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static io.github.piscescup.fabricmc.carpetgui.References.LOGGER;

/** Client-local rule collections with membership, staging, commits and push state. */
final class RuleGroupStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Data data;

    private RuleGroupStore() {
    }

    static synchronized List<Group> groups() {
        load();
        return List.copyOf(data.groups);
    }

    static synchronized Group find(String id) {
        load();
        return data.groups.stream().filter(group -> group.id.equals(id)).findFirst().orElse(null);
    }

    static synchronized Group create(String name, String tag) {
        load();
        Group group = new Group();
        group.id = UUID.randomUUID().toString();
        group.name = clipped(name.strip(), 64);
        group.tag = clipped(tag.strip(), 32);
        data.groups.add(group);
        save();
        return group;
    }

    /** Membership is independent of Git add; a new member remains untracked until staged. */
    static synchronized void addMember(Group group, String ruleId) {
        group.members.add(ruleId);
        save();
    }

    static synchronized void removeMember(Group group, String ruleId) {
        group.members.remove(ruleId);
        group.head.remove(ruleId);
        group.staged.remove(ruleId);
        save();
    }

    /** Git-like add: stage the current value only when it differs from HEAD. */
    static synchronized void stage(Group group, String ruleId, String value) {
        if (!group.members.contains(ruleId)) return;
        if (value.equals(group.head.get(ruleId))) group.staged.remove(ruleId);
        else group.staged.put(ruleId, value);
        save();
    }

    static synchronized int stageAll(Group group, Map<String, String> values) {
        int count = 0;
        for (String ruleId : group.members) {
            String value = values.get(ruleId);
            if (value == null) continue;
            if (value.equals(group.head.get(ruleId))) {
                group.staged.remove(ruleId);
                continue;
            }
            group.staged.put(ruleId, value);
            count++;
        }
        save();
        return count;
    }

    static synchronized void unstage(Group group, String ruleId) {
        group.staged.remove(ruleId);
        save();
    }

    static synchronized void clearStage(Group group) {
        group.staged.clear();
        save();
    }

    static synchronized boolean commit(Group group, String message) {
        String cleanMessage = clipped(message.strip(), 120);
        if (cleanMessage.isEmpty() || group.staged.isEmpty()) return false;
        group.head.putAll(group.staged);
        group.staged.clear();
        group.revision++;
        long time = System.currentTimeMillis();
        group.history.add(0, new Commit(group.revision, time, cleanMessage, new LinkedHashMap<>(group.head)));
        if (group.history.size() > 40) group.history.subList(40, group.history.size()).clear();
        save();
        return true;
    }

    static synchronized void markPushed(Group group) {
        group.pushedRevision = group.revision;
        save();
    }

    private static String clipped(String text, int length) {
        return text.length() <= length ? text : text.substring(0, length);
    }

    private static void load() {
        if (data != null) return;
        Path file = file();
        if (!Files.isRegularFile(file)) {
            data = new Data();
            return;
        }
        try {
            data = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Data.class);
            if (data == null) data = new Data();
            data.sanitize();
        } catch (Exception failure) {
            data = new Data();
            LOGGER.warn("Cannot read Carpet GUI rule groups from {}", file, failure);
        }
    }

    private static void save() {
        Path file = file();
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(temporary, GSON.toJson(data), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException failure) {
            LOGGER.warn("Cannot save Carpet GUI rule groups to {}", file, failure);
        }
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("carpet-gui").resolve("rule-groups.json");
    }

    static final class Group {
        private String id = "";
        private String name = "";
        private String tag = "";
        private int revision;
        private int pushedRevision;
        private Set<String> members = new LinkedHashSet<>();
        private Map<String, String> head = new LinkedHashMap<>();
        private Map<String, String> staged = new LinkedHashMap<>();
        private List<Commit> history = new ArrayList<>();

        // Version-one compatibility fields.
        @SuppressWarnings("unused") private Set<String> removed;
        @SuppressWarnings("unused") private long committedAt;

        String id() { return id; }
        String name() { return name; }
        String tag() { return tag; }
        int revision() { return revision; }
        int pushedRevision() { return pushedRevision; }
        int commitsAhead() { return Math.max(0, revision - pushedRevision); }
        Set<String> members() { return Collections.unmodifiableSet(new LinkedHashSet<>(members)); }
        Map<String, String> head() { return Collections.unmodifiableMap(new LinkedHashMap<>(head)); }
        Map<String, String> staged() { return Collections.unmodifiableMap(new LinkedHashMap<>(staged)); }
        List<Commit> history() { return List.copyOf(history); }
        boolean tracked(String ruleId) { return head.containsKey(ruleId); }
        String headValue(String ruleId) { return head.get(ruleId); }
        boolean isStaged(String ruleId) { return staged.containsKey(ruleId); }
        String stagedValue(String ruleId) { return staged.get(ruleId); }

        private void sanitize(int sourceFormat) {
            if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
            name = name == null ? "" : clipped(name.strip(), 64);
            tag = tag == null ? "" : clipped(tag.strip(), 32);
            members = members == null ? new LinkedHashSet<>() : new LinkedHashSet<>(members);
            head = head == null ? new LinkedHashMap<>() : new LinkedHashMap<>(head);
            staged = staged == null ? new LinkedHashMap<>() : new LinkedHashMap<>(staged);
            history = history == null ? new ArrayList<>() : new ArrayList<>(history);
            head.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
            staged.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
            // Version one used HEAD/staged as membership and staged removals separately.
            members.addAll(head.keySet());
            members.addAll(staged.keySet());
            if (removed != null) {
                members.removeAll(removed);
                removed.forEach(ruleId -> { head.remove(ruleId); staged.remove(ruleId); });
            }
            members.removeIf(ruleId -> ruleId == null);
            head.keySet().retainAll(members);
            staged.keySet().retainAll(members);
            history.removeIf(commit -> commit == null || commit.values == null);
            history.forEach(Commit::sanitize);
            // Formats 1/2 treated newly grouped rules as if they were already at HEAD.
            if (sourceFormat < 3) {
                if (history.isEmpty()) head.clear();
                else head.keySet().retainAll(history.getFirst().values.keySet());
            }
            revision = Math.max(0, revision);
            pushedRevision = Math.clamp(pushedRevision, 0, revision);
            removed = null;
            committedAt = 0;
        }
    }

    static final class Commit {
        private int revision;
        private long time;
        private String message;
        private Map<String, String> values;

        private Commit() {
        }

        private Commit(int revision, long time, String message, Map<String, String> values) {
            this.revision = revision;
            this.time = time;
            this.message = message;
            this.values = values;
        }

        int revision() { return revision; }
        long time() { return time; }
        String message() { return message; }
        int ruleCount() { return values.size(); }

        private void sanitize() {
            if (message == null || message.isBlank()) message = "r" + revision;
            values = new LinkedHashMap<>(values);
            values.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        }
    }

    private static final class Data {
        private int format = 3;
        private List<Group> groups = new ArrayList<>();

        private void sanitize() {
            if (groups == null) groups = new ArrayList<>();
            groups.removeIf(group -> group == null);
            int sourceFormat = format;
            groups.forEach(group -> group.sanitize(sourceFormat));
            format = 3;
        }
    }
}

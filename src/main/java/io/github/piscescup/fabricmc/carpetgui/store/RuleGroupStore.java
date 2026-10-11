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

package io.github.piscescup.fabricmc.carpetgui.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Client-local rule collections with membership, staging, commits and push state.
 */
public final class RuleGroupStore {
    private static final Logger LOGGER = LogManager.getLogger(RuleGroupStore.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();
    private static Repository repository;

    private static Repository repository() {
        if (repository == null) repository = new Repository(directory(), FabricLoader.getInstance().getGameDir().resolve("carpet-gui"));
        return repository;
    }

    private static Data data() {
        return repository().data();
    }

    /**
     * Global storage root; presets and personal preferences do not follow the active context.
     */
    public static Path directory() {
        return FabricLoader.getInstance()
            .getConfigDir()
            .resolve("carpet-gui");
    }

    public static synchronized void switchContext(WorkspaceContext context) {
        WorkspaceSession.begin(context);
        repository().switchContext(context);
    }

    private static boolean owns(Group group) {
        return data().groups.contains(group);
    }

    public static synchronized boolean legacyAvailable() {
        return repository().legacyAvailable();
    }

    public static synchronized boolean storageProblem() {
        return repository().storageProblem();
    }

    /**
     * Copies legacy groups once per context, leaving the original global file untouched.
     */
    public static synchronized int copyLegacyGroups(String author) throws IOException {
        return repository().copyLegacyGroups(author, System.currentTimeMillis());
    }

    private RuleGroupStore() {
    }

    public static synchronized List<Group> groups() {
        load();
        return List.copyOf(data().groups);
    }

    public static synchronized Group find(String id) {
        load();
        return data().groups.stream()
            .filter(group -> group.id.equals(id))
            .findFirst()
            .orElse(null);
    }

    public static synchronized Group create(String name, String tag) {
        load();
        Group group = new Group();
        group.id = UUID.randomUUID()
            .toString();
        group.name = clipped(name.strip(), 64);
        group.tag = clipped(tag.strip(), 32);
        data().groups.add(group);
        save();
        return group;
    }

    /**
     * Imports into a fresh local group; never replaces an existing group or edits a server.
     */
    public static synchronized Group importPreset(RulePreset preset, String author) {
        load();
        Group group = presetGroup(preset, author, System.currentTimeMillis());
        data().groups.add(group);
        save();
        return group;
    }

    static Group presetGroup(RulePreset preset, String author, long time) {
        Group group = new Group();
        group.id = UUID.randomUUID()
            .toString();
        group.name = preset.name();
        group.tag = preset.tag();
        preset.rules()
            .forEach((key, value) -> {
                group.addMember(key);
                group.stage(key, value);
            });
        group.commit("Import preset: " + clipped(preset.name(), 64), author == null ? "" : clipped(author, 64), time);
        return group;
    }

    /**
     * Membership is independent of Git add; a new member remains untracked until staged.
     */
    public static synchronized void addMember(Group group, String ruleId) {
        if (!owns(group)) return;
        group.addMember(ruleId);
        save();
    }

    public static synchronized void removeMember(Group group, String ruleId) {
        if (!owns(group)) return;
        group.removeMember(ruleId);
        save();
    }

    /**
     * Git-like add: stage the current value only when it differs from HEAD.
     */
    public static synchronized void stage(Group group, String ruleId, String value) {
        if (!owns(group)) return;
        group.stage(ruleId, value);
        save();
    }

    public static synchronized int stageAll(Group group, Map<String, String> values) {
        if (!owns(group)) return 0;
        group.stagedDeletions.clear();
        group.head.keySet()
            .stream()
            .filter(key -> !group.members.contains(key))
            .forEach(group.stagedDeletions::add);
        int count = group.stagedDeletions.size();
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

    public static synchronized void unstage(Group group, String ruleId) {
        if (!owns(group)) return;
        group.staged.remove(ruleId);
        group.stagedDeletions.remove(ruleId);
        save();
    }

    public static synchronized void stageRemoval(Group group, String ruleId) {
        if (!owns(group)) return;
        if (group.head.containsKey(ruleId) && !group.members.contains(ruleId)) {
            group.stagedDeletions.add(ruleId);
            save();
        }
    }

    public static synchronized void clearStage(Group group) {
        if (!owns(group)) return;
        group.staged.clear();
        group.stagedDeletions.clear();
        save();
    }

    public static synchronized boolean commit(Group group, String message) {
        return commit(group, message, "");
    }

    /**
     * Creates a commit snapshot and records the display name of its author.
     */
    public static synchronized boolean commit(Group group, String message, String author) {
        if (!owns(group)) return false;
        String cleanMessage = clipped(message.strip(), 120);
        String cleanAuthor = author == null ? "" : clipped(author.strip(), 64);
        if (!group.commit(cleanMessage, cleanAuthor, System.currentTimeMillis())) return false;
        save();
        return true;
    }

    public static synchronized void markPushed(Group group) {
        markPushed(group, group.revision);
    }

    /**
     * Acknowledges only the revision actually sent, not commits made while a push was pending.
     */
    public static synchronized void markPushed(Group group, int revision) {
        if (!owns(group)) return;
        group.acknowledge(revision);
        save();
    }

    public static String clipped(String text, int length) {
        return text.length() <= length ? text : text.substring(0, length);
    }

    public static synchronized void load() {
        repository().data();
    }

    public static synchronized void save() {
        repository().save();
    }

    public static synchronized Path file() {
        return repository().file();
    }

    private static Data read(Path file) throws IOException {
        try {
            var json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (json.isJsonObject() && !json.getAsJsonObject()
                .has("format")) {
                json.getAsJsonObject()
                    .addProperty("format", 1);
            }
            Data result = GSON.fromJson(json, Data.class);
            if (result == null) throw new IllegalArgumentException("Missing group data");
            result.sanitize();
            return result;
        } catch (RuntimeException failure) {
            throw new IOException("Invalid rule group file: " + file, failure);
        }
    }

    /**
     * A migrated snapshot starts locally pending; membership and unfinished staging are preserved.
     */
    static Group legacyCopy(Group source, String author, long time) {
        Group result = GSON.fromJson(GSON.toJson(source), Group.class);
        result.id = UUID.randomUUID()
            .toString();
        result.revision = source.head.isEmpty() ? 0 : 1;
        result.pushedRevision = 0;
        result.history.clear();
        if (result.revision != 0) {
            result.history.add(new Commit(
                1, time, "Copy legacy group: " + clipped(source.name, 64),
                author == null ? "" : clipped(author, 64), new LinkedHashMap<>(source.head), new LinkedHashMap<>()
            ));
        }
        return result;
    }

    /**
     * Instance-based disk layer, also used by tests without a running Minecraft client.
     */
    static final class Repository {
        private final Path root;
        private final Path multiplayerRoot;
        private WorkspaceContext context = WorkspaceContext.offline();
        private Data data;
        private boolean unreadable;
        private boolean failedSave;
        // A failed save must not discard edits when the player switches environments.
        private final Map<Path, String> pending = new LinkedHashMap<>();

        Repository(Path root, Path multiplayerRoot) {
            this.root = root;
            this.multiplayerRoot = multiplayerRoot;
        }

        Path file() {
            return context.file(root, multiplayerRoot);
        }

        List<Group> groups() {
            return data().groups;
        }

        boolean storageProblem() {
            data();
            return unreadable || failedSave;
        }

        void switchContext(WorkspaceContext next) {
            if (data != null && !save()) pending.put(file(), GSON.toJson(data));
            context = next;
            data = null;
            unreadable = false;
            failedSave = false;
        }

        Data data() {
            if (data != null) return data;
            Path file = file();
            try {
                if (pending.containsKey(file)) {
                    data = GSON.fromJson(pending.get(file), Data.class);
                    data.sanitize();
                    failedSave = true;
                    // Do not overwrite an unreadable on-disk file after returning to this context.
                    try {
                        read(file);
                    } catch (NoSuchFileException missing) { /* Pending new file. */ }
                } else {
                    try {
                        data = read(file);
                    } catch (NoSuchFileException missing) {
                        data = new Data();
                    }
                }
            } catch (IOException failure) {
                if (data == null) data = new Data();
                unreadable = true;
                LOGGER.warn("Cannot read Carpet GUI rule groups from {}; refusing to overwrite it", file, failure);
            }
            return data;
        }

        boolean save() {
            if (data == null) return true;
            if (unreadable) return false;
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
                pending.remove(file);
                failedSave = false;
                return true;
            } catch (IOException failure) {
                failedSave = true;
                LOGGER.warn("Cannot save Carpet GUI rule groups to {}", file, failure);
                return false;
            }
        }

        boolean legacyAvailable() {
            return context.connected() && !data().legacyCopied && Files.isRegularFile(root.resolve("rule-groups.json"));
        }

        int copyLegacyGroups(String author, long time) throws IOException {
            if (!legacyAvailable()) return 0;
            Data source = read(root.resolve("rule-groups.json"));
            var copies = source.groups.stream()
                .map(group -> legacyCopy(group, author, time))
                .toList();
            data().groups.addAll(copies);
            data.legacyCopied = true;
            if (!save()) {
                data.groups.removeAll(copies);
                data.legacyCopied = false;
                throw new IOException("Cannot save migrated groups");
            }
            return copies.size();
        }
    }

    public static final class Group {
        private String id = "";
        private String name = "";
        private String tag = "";
        private int revision;
        private int pushedRevision;
        private Set<String> members = new LinkedHashSet<>();
        private Map<String, String> head = new LinkedHashMap<>();
        private Map<String, String> staged = new LinkedHashMap<>();
        private Set<String> stagedDeletions = new LinkedHashSet<>();
        private List<Commit> history = new ArrayList<>();

        // Pure state transitions are separate from disk I/O so both UI generations
        // and regression tests use exactly the same commit semantics.
        void addMember(String ruleId) {
            members.add(ruleId);
            stagedDeletions.remove(ruleId);
        }

        void removeMember(String ruleId) {
            members.remove(ruleId);
            if (head.containsKey(ruleId)) stagedDeletions.add(ruleId);
            staged.remove(ruleId);
        }

        void stage(String ruleId, String value) {
            if (!members.contains(ruleId)) return;
            if (value.equals(head.get(ruleId))) {
                staged.remove(ruleId);
            } else {
                staged.put(ruleId, value);
            }
        }

        boolean commit(String message, String author, long time) {
            if (message.isBlank() || !hasStagedChanges()) return false;
            Map<String, String> previousValues = new LinkedHashMap<>(head);
            stagedDeletions.forEach(head::remove);
            head.putAll(staged);
            staged.clear();
            stagedDeletions.clear();
            history.add(0, new Commit(++revision, time, message, author, new LinkedHashMap<>(head), previousValues));
            if (history.size() > 40) {
                history.subList(40, history.size())
                    .clear();
            }
            return true;
        }

        void acknowledge(int sentRevision) {
            pushedRevision = Math.max(pushedRevision, Math.clamp(sentRevision, 0, revision));
        }

        // Version-one compatibility fields.
        @SuppressWarnings("unused")
        private Set<String> removed;
        @SuppressWarnings("unused")
        private long committedAt;

        public String id() {
            return id;
        }

        public String name() {
            return name;
        }

        public String tag() {
            return tag;
        }

        public int revision() {
            return revision;
        }

        public int pushedRevision() {
            return pushedRevision;
        }

        public int commitsAhead() {
            return Math.max(0, revision - pushedRevision);
        }

        public Set<String> members() {
            return Collections.unmodifiableSet(new LinkedHashSet<>(members));
        }

        public Map<String, String> head() {
            return Collections.unmodifiableMap(new LinkedHashMap<>(head));
        }

        public Map<String, String> staged() {
            return Collections.unmodifiableMap(new LinkedHashMap<>(staged));
        }

        public Set<String> stagedDeletions() {
            return Collections.unmodifiableSet(new LinkedHashSet<>(stagedDeletions));
        }

        public int stagedCount() {
            return staged.size() + stagedDeletions.size();
        }

        public boolean hasStagedChanges() {
            return stagedCount() != 0;
        }

        public Set<String> unstagedDeletions() {
            var result = new LinkedHashSet<>(head.keySet());
            result.removeAll(members);
            result.removeAll(stagedDeletions);
            return Collections.unmodifiableSet(result);
        }

        public List<Commit> history() {
            return List.copyOf(history);
        }

        public boolean tracked(String ruleId) {
            return head.containsKey(ruleId);
        }

        public String headValue(String ruleId) {
            return head.get(ruleId);
        }

        public boolean isStaged(String ruleId) {
            return staged.containsKey(ruleId);
        }

        public String stagedValue(String ruleId) {
            return staged.get(ruleId);
        }

        private void sanitize(int sourceFormat) {
            if (id == null || id.isBlank()) {
                id = UUID.randomUUID()
                    .toString();
            }
            name = name == null ? "" : clipped(name.strip(), 64);
            tag = tag == null ? "" : clipped(tag.strip(), 32);
            members = members == null ? new LinkedHashSet<>() : new LinkedHashSet<>(members);
            head = head == null ? new LinkedHashMap<>() : new LinkedHashMap<>(head);
            staged = staged == null ? new LinkedHashMap<>() : new LinkedHashMap<>(staged);
            stagedDeletions = stagedDeletions == null ? new LinkedHashSet<>() : new LinkedHashSet<>(stagedDeletions);
            history = history == null ? new ArrayList<>() : new ArrayList<>(history);
            head.entrySet()
                .removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
            staged.entrySet()
                .removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
            // Version one used HEAD/staged as membership and staged removals separately.
            if (sourceFormat < 3) members.addAll(head.keySet());
            members.addAll(staged.keySet());
            if (removed != null) {
                members.removeAll(removed);
                removed.forEach(ruleId -> {
                    head.remove(ruleId);
                    staged.remove(ruleId);
                });
            }
            members.removeIf(ruleId -> ruleId == null);
            staged.keySet()
                .retainAll(members);
            history.removeIf(commit -> commit == null || commit.values == null);
            history.forEach(Commit::sanitize);
            // Formats 1/2 treated newly grouped rules as if they were already at HEAD.
            if (sourceFormat < 3) {
                if (history.isEmpty()) {
                    head.clear();
                } else {
                    head.keySet()
                        .retainAll(history.getFirst().values.keySet());
                }
            }
            if (sourceFormat < 4 && !history.isEmpty()) {
                // Older removal code silently mutated HEAD. Recover the committed snapshot
                // and represent missing members as a pending deletion instead.
                head = new LinkedHashMap<>(history.getFirst().values);
                head.keySet()
                    .stream()
                    .filter(key -> !members.contains(key))
                    .forEach(stagedDeletions::add);
            }
            stagedDeletions.removeIf(key -> key == null || !head.containsKey(key) || members.contains(key));
            revision = Math.max(0, revision);
            pushedRevision = Math.clamp(pushedRevision, 0, revision);
            removed = null;
            committedAt = 0;
        }
    }

    public static final
    class Commit {
        private int revision;
        private long time;
        private String message;
        private String author;
        private Map<String, String> values;
        private Map<String, String> previousValues;

        public Commit() {
        }

        private Commit(
            int revision, long time, String message, String author, Map<String, String> values,
            Map<String, String> previousValues
        )
        {
            this.revision = revision;
            this.time = time;
            this.message = message;
            this.author = author;
            this.values = values;
            this.previousValues = previousValues;
        }

        public int revision() {
            return revision;
        }

        public long time() {
            return time;
        }

        public String message() {
            return message;
        }

        public String author() {
            return author;
        }

        /**
         * Returns the immutable rule snapshot captured by this commit.
         */
        public Map<String, String> values() {
            return Collections.unmodifiableMap(values);
        }

        /**
         * Null only for legacy commits whose parent snapshot was not stored.
         */
        public Map<String, String> previousValues() {
            return previousValues == null ? null : Collections.unmodifiableMap(previousValues);
        }

        public int ruleCount() {
            return values.size();
        }

        public void sanitize() {
            if (message == null || message.isBlank()) message = "r" + revision;
            if (author == null) author = "";
            values = new LinkedHashMap<>(values);
            values.entrySet()
                .removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
            if (previousValues != null) {
                previousValues = new LinkedHashMap<>(previousValues);
                previousValues.entrySet()
                    .removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
            }
        }
    }

    public static
    final class Data {
        private int format = 4;
        private boolean legacyCopied;
        private List<Group> groups = new ArrayList<>();

        public void sanitize() {
            if (groups == null) groups = new ArrayList<>();
            groups.removeIf(group -> group == null);
            int sourceFormat = format;
            groups.forEach(group -> group.sanitize(sourceFormat));
            format = 4;
        }
    }
}

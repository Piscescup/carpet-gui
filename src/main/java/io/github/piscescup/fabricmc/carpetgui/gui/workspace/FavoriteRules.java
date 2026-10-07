package io.github.piscescup.fabricmc.carpetgui.gui.workspace;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.Set;

import static io.github.piscescup.fabricmc.carpetgui.References.LOGGER;

/** Client-local persistent favorites, keyed by a rule's stable state ID. */
final class FavoriteRules {
    private static final Set<String> RULE_IDS = new LinkedHashSet<>();
    private static boolean loaded;

    private FavoriteRules() {
    }

    static synchronized boolean contains(String ruleId) {
        load();
        return RULE_IDS.contains(ruleId);
    }

    static synchronized boolean toggle(String ruleId) {
        load();
        boolean favorite;
        if (RULE_IDS.remove(ruleId)) {
            favorite = false;
        } else {
            RULE_IDS.add(ruleId);
            favorite = true;
        }
        save();
        return favorite;
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        Path file = file();
        if (!Files.isRegularFile(file)) return;
        try {
            Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                .map(String::strip)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .forEach(RULE_IDS::add);
        } catch (IOException failure) {
            LOGGER.warn("Cannot read Carpet GUI favorites from {}", file, failure);
        }
    }

    private static void save() {
        Path file = file();
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            Files.write(temporary, RULE_IDS.stream().sorted().toList(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException failure) {
            LOGGER.warn("Cannot save Carpet GUI favorites to {}", file, failure);
        }
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("carpet-gui").resolve("favorites.txt");
    }
}

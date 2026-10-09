package io.github.piscescup.fabricmc.carpetgui.integration.carpet;

import io.github.piscescup.fabricmc.carpetgui.adapter.Accessor;
import io.github.piscescup.fabricmc.carpetgui.adapter.CarpetAddonAdapter;
import io.github.piscescup.fabricmc.carpetgui.api.CarpetModRulesApi;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.BiConsumer;

import static io.github.piscescup.fabricmc.carpetgui.References.*;

public final class CarpetSeriesMods {
    public static Path adapterConfigDirectory() {
        return FABRIC_LOADER.getConfigDir()
            .resolve(MOD_ID)
            .resolve("carpet_series_addon_mods_cfg");
    }

    /** Called during mod initialization, after Fabric's configuration directory is available. */
    public static void initialize() {
        List<CarpetAddonAdapter> loaded = loadAdapters(
            adapterConfigDirectory(),
            GSON,
            (file, failure) ->
                LOGGER.warn("Cannot load Carpet adapter configuration {}", file, failure)
        );
        CARPET_SERIES.clear();
        CARPET_SERIES.addAll(loaded);
    }

    public static void saveAdapterAsJson(CarpetAddonAdapter adapter) throws IOException {
        saveAdapterAsJson(adapterConfigDirectory(), adapter, GSON);
    }

    /** Explicit saves replace a file atomically where supported. Startup never overwrites existing files. */
    public static void saveAdapterAsJson(Path directory, CarpetAddonAdapter adapter, Gson gson) throws IOException {
        Path destination = adapterFile(directory, adapter.carpetModId());
        String json = gson.toJson(adapter, CarpetAddonAdapter.class) + System.lineSeparator();
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, ".adapter-", ".tmp");
        try {
            Files.writeString(temporary, json, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /** Loads file overrides and additional adapters, retaining defaults for missing or invalid files.
     * Does not generate or modify configuration files. */
    public static List<CarpetAddonAdapter> loadAdapters(Path directory, Gson gson, BiConsumer<Path, Exception> onError) {
        var adapters = new LinkedHashMap<String, CarpetAddonAdapter>();
        for (CarpetAddonAdapter adapter : defaultAdapters()) adapters.put(adapter.carpetModId(), adapter);
        try {
            Files.createDirectories(directory);
        } catch (IOException failure) {
            onError.accept(directory, failure);
            return sorted(adapters.values());
        }
        try (var files = Files.list(directory)) {
            for (Path file : files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList()
            ) {
                try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    CarpetAddonAdapter adapter = gson.fromJson(reader, CarpetAddonAdapter.class);
                    if (adapter == null) throw new JsonParseException("Adapter configuration cannot be null");

                    if (!file.getFileName().equals(adapterFile(directory, adapter.carpetModId()).getFileName())) {
                        throw new JsonParseException("Adapter filename must match its modId");
                    }
                    adapters.put(adapter.carpetModId(), adapter);
                } catch (IOException | RuntimeException failure) {
                    onError.accept(file, failure);
                }
            }
        } catch (IOException failure) {
            onError.accept(directory, failure);
        }
        return sorted(adapters.values());
    }

    private static Path adapterFile(Path directory, String modId) {
        if (modId == null || Identifier.isValidNamespace(modId)) {
            throw new IllegalArgumentException("Invalid adapter Mod ID: " + modId);
        }
        return directory.resolve(modId + ".json");
    }

    private static List<CarpetAddonAdapter> sorted(Collection<CarpetAddonAdapter> adapters) {
        return adapters.stream().sorted(Comparator.comparing(CarpetAddonAdapter::carpetModId)).toList();
    }

    /**
     * Compatibility adapters for released addons which cannot implement Carpet GUI's APIs themselves.
     */
    public static final List<CarpetModRulesApi> CARPET_SERIES = new ArrayList<>(defaultAdapters());

    public static List<CarpetAddonAdapter> defaultAdapters() {
        return List.of(
            CarpetAddonAdapter.annotationBuilder("carpet-pry-addition", "me.primaryuan.carpet")
                .carpetExtensionClassName("CarpetPrimaryuanServer")
                .settingsClassName("CarpetPrimaryuanSettings")
                .ruleAnnotationClassName("settings.Rule")
                .build(),
            CarpetAddonAdapter.ruleBuilder("carpet-igny-addition", "com.liuyue.igny")
                .carpetExtensionClassName("IGNYServer")
                .settingsClassName("IGNYSettings")
                .carpetRuleClassName("rule.RuleContext")
                .rulesAccessor(Accessor.ofStaticField("RULES"))
                .ruleNameAccessor(Accessor.ofInstanceMethod("getName"))
                .build(),
            CarpetAddonAdapter.annotationBuilder("carpet-tis-addition", "carpettisaddition")
                .carpetExtensionClassName("CarpetTISAdditionServer")
                .settingsClassName("CarpetTISAdditionSettings")
                .ruleAnnotationClassName("settings.Rule")
                .build(),
            CarpetAddonAdapter.ruleBuilder("carpet-org-addition", "boat.carpetorgaddition")
                .carpetExtensionClassName("CarpetOrgAdditionExtension")
                .settingsClassName("CarpetOrgAdditionSettings")
                .carpetRuleClassName("rule.RuleContext")
                .rulesAccessor(Accessor.ofStaticField("RULES"))
                .ruleNameAccessor(Accessor.ofInstanceMethod("getName"))
                .build(),
            CarpetAddonAdapter.annotationBuilder("carpet-ams-addition", "carpetamsaddition")
                .carpetExtensionClassName("CarpetAMSAdditionServer")
                .settingsClassName("CarpetAMSAdditionSettings")
                .ruleAnnotationClassName("settings.Rule")
                .build(),
            CarpetAddonAdapter.annotationBuilder("carpet-extra-extras", "net.thedustbuster.cee.server")
                .carpetExtensionClassName("CarpetExtraExtrasServer")
                .extensionAccessor(Accessor.ofRegisteredExtension())
                .settingsClassName("CarpetExtraExtrasSettings")
                .onCarpetRuleAnnotation()
                .build(),
            CarpetAddonAdapter.annotationBuilder("gca", "dev.dubhe.gugle.carpet")
                .carpetExtensionClassName("GcaExtension")
                .extensionAccessor(Accessor.ofRegisteredExtension())
                .settingsClassName("GcaSetting")
                .onCarpetRuleAnnotation()
                .build(),
            CarpetAddonAdapter.annotationBuilder("carpet-rof-addition", "com.carpet.rof")
                .carpetExtensionClassName("ROFCarpetServer")
                .extensionAccessor(Accessor.ofRegisteredExtension())
                .settingsClassName("ROFSettings")
                .settingsClassesAccessor(Accessor.ofStaticField("ruleClasses"))
                .onCarpetRuleAnnotation()
                .build(),
            CarpetAddonAdapter.managerBuilder("cca", "com.github.crystal0404.mods.crystalcarpetaddition")
                .managerClassName("CCAExtension")
                .managerAccessor(Accessor.ofStaticField("CCASettingsManager"))
                .build()
        );
    }

    static {
        CARPET_SERIES.sort(Comparator.comparing(CarpetModRulesApi::carpetModId));
    }

}

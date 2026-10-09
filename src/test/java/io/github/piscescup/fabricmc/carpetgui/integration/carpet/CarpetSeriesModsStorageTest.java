package io.github.piscescup.fabricmc.carpetgui.integration.carpet;

import com.google.gson.*;
import io.github.piscescup.fabricmc.carpetgui.adapter.*;
import io.github.piscescup.fabricmc.carpetgui.adapter.codec.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CarpetSeriesModsStorageTest {
    @TempDir Path directory;
    private final Gson gson = new GsonBuilder()
        .registerTypeHierarchyAdapter(PackageRef.class, new PackageRefJsonCodec())
        .registerTypeHierarchyAdapter(Accessor.class, new AccessorJsonCodec())
        .registerTypeHierarchyAdapter(CarpetAddonAdapter.class, new CarpetAddonAdapterJsonCodec())
        .setPrettyPrinting().create();

    @Test void roundTripsAllBuiltinAdaptersWithoutLoadingAddonClassesOrRuntimeCaches() {
        for (CarpetAddonAdapter source : CarpetSeriesMods.defaultAdapters()) {
            String json = gson.toJson(source);
            CarpetAddonAdapter restored = gson.fromJson(json, CarpetAddonAdapter.class);
            assertEquals(source.getClass(), restored.getClass());
            assertEquals(gson.toJsonTree(source), gson.toJsonTree(restored));
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            assertFalse(object.has("extension"));
            assertFalse(object.has("manager"));
            assertFalse(object.has("warnings"));
        }
    }

    @Test void usesDefaultsWithoutGeneratingFiles() throws Exception {
        var failures = new ArrayList<Exception>();
        var first = CarpetSeriesMods.loadAdapters(directory, gson, (file, failure) -> failures.add(failure));
        assertEquals(9, first.size());
        try (var files = Files.list(directory)) { assertEquals(0, files.count()); }
        var second = CarpetSeriesMods.loadAdapters(directory, gson, (file, failure) -> failures.add(failure));
        assertEquals(9, second.size());
        assertTrue(failures.isEmpty(), failures.toString());
    }

    @Test void missingDirectoryUsesDefaultsWithoutGeneratingFiles() throws Exception {
        Path missing = directory.resolve("missing");
        var loaded = CarpetSeriesMods.loadAdapters(missing, gson, (file, failure) -> fail(failure));
        assertEquals(9, loaded.size());
        try (var files = Files.list(missing)) { assertEquals(0, files.count()); }
    }

    @Test void loadsOverrideAndAdditionalAdapterWithoutOverwritingUserFiles() throws Exception {
        var replacement = manager("cca", "ChangedExtension");
        CarpetSeriesMods.saveAdapterAsJson(directory, replacement, gson);
        CarpetSeriesMods.saveAdapterAsJson(directory, manager("custom-addon", "CustomExtension"), gson);
        String before = Files.readString(directory.resolve("cca.json"));
        var loaded = CarpetSeriesMods.loadAdapters(directory, gson, (file, failure) -> fail(failure));
        assertEquals(10, loaded.size());
        var cca = loaded.stream().filter(adapter -> adapter.carpetModId().equals("cca")).findFirst().orElseThrow();
        assertEquals("ChangedExtension", ((ManagerCarpetAddonAdapter) cca).managerClassRef().name());
        assertEquals(before, Files.readString(directory.resolve("cca.json")));
    }

    @Test void malformedFileKeepsDefaultAndDoesNotBlockOtherFiles() throws Exception {
        Files.writeString(directory.resolve("cca.json"), "{broken");
        CarpetSeriesMods.saveAdapterAsJson(directory, manager("custom-addon", "CustomExtension"), gson);
        var failedFiles = new ArrayList<Path>();
        var loaded = CarpetSeriesMods.loadAdapters(directory, gson, (file, failure) -> failedFiles.add(file));
        assertEquals(10, loaded.size());
        assertEquals(List.of(directory.resolve("cca.json")), failedFiles);
        assertEquals("{broken", Files.readString(directory.resolve("cca.json")));
        var cca = (ManagerCarpetAddonAdapter) loaded.stream()
            .filter(adapter -> adapter.carpetModId().equals("cca")).findFirst().orElseThrow();
        assertEquals("CCAExtension", cca.managerClassRef().name());
    }

    @Test void explicitSaveReplacesExistingFileAndLeavesNoTemporaryFiles() throws Exception {
        CarpetSeriesMods.saveAdapterAsJson(directory, manager("custom-addon", "First"), gson);
        CarpetSeriesMods.saveAdapterAsJson(directory, manager("custom-addon", "Second"), gson);
        var loaded = gson.fromJson(Files.readString(directory.resolve("custom-addon.json")), CarpetAddonAdapter.class);
        assertEquals("Second", ((ManagerCarpetAddonAdapter) loaded).managerClassRef().name());
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }

    @Test void rejectsPathTraversalAndFilenameMismatch() throws Exception {
        assertThrows(IllegalArgumentException.class,
            () -> CarpetSeriesMods.saveAdapterAsJson(directory, manager("../escape", "X"), gson));
        Files.writeString(directory.resolve("wrong.json"), gson.toJson(manager("custom-addon", "X")));
        var failedFiles = new ArrayList<Path>();
        var loaded = CarpetSeriesMods.loadAdapters(directory, gson, (file, failure) -> failedFiles.add(file));
        assertEquals(9, loaded.size());
        assertEquals(List.of(directory.resolve("wrong.json")), failedFiles);
    }

    @Test void rejectsUnsupportedVersionAndMissingRequiredFields() {
        JsonObject json = gson.toJsonTree(manager("custom-addon", "X")).getAsJsonObject();
        json.addProperty("schemaVersion", 2);
        assertThrows(JsonParseException.class, () -> gson.fromJson(json, CarpetAddonAdapter.class));
        json.addProperty("schemaVersion", 1);
        json.remove("managerAccessor");
        assertThrows(JsonParseException.class, () -> gson.fromJson(json, CarpetAddonAdapter.class));
    }

    @Test void directoryFailureRetainsBuiltins() throws Exception {
        Path file = directory.resolve("not-a-directory");
        Files.writeString(file, "occupied");
        var failures = new ArrayList<Exception>();
        assertEquals(9, CarpetSeriesMods.loadAdapters(file, gson, (path, failure) -> failures.add(failure)).size());
        assertEquals(1, failures.size());
    }

    private CarpetAddonAdapter manager(String id, String owner) {
        return CarpetAddonAdapter.managerBuilder(id, "test.addon")
            .managerClassName(owner).managerAccessor(Accessor.ofStaticField("MANAGER")).build();
    }
}

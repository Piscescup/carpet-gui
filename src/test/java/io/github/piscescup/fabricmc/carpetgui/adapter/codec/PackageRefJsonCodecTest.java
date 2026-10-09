package io.github.piscescup.fabricmc.carpetgui.adapter.codec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import io.github.piscescup.fabricmc.carpetgui.adapter.PackageRef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PackageRefJsonCodecTest {
    private final Gson gson = new GsonBuilder()
        .registerTypeHierarchyAdapter(PackageRef.class, new PackageRefJsonCodec()).create();

    @Test
    void roundTripsRelativeNameWithoutResolvingIt() {
        PackageRef source = PackageRef.relative("settings.Rule");
        String json = gson.toJson(source);
        assertEquals("settings.Rule", gson.fromJson(json, PackageRef.class).name());
        PackageRef restored = gson.fromJson(json, PackageRef.class);
        assertFalse(restored.absolute());
        assertEquals("com.example.settings.Rule", restored.toCanonicalPackage("com.example"));
    }

    @Test
    void roundTripsAbsoluteNestedClassName() {
        PackageRef source = PackageRef.absolute("com.example.Outer$Inner");
        PackageRef restored = gson.fromJson(gson.toJson(source, PackageRef.class), PackageRef.class);
        assertTrue(restored.absolute());
        assertEquals(source.name(), restored.toCanonicalPackage(null));
    }

    @Test
    void rejectsMalformedDescriptions() {
        for (String json : new String[]{"[]", "{}", "{\"name\":12,\"absolute\":true}",
            "{\"name\":\"X\",\"absolute\":\"false\"}", "{\"name\":\" \",\"absolute\":false}"}) {
            assertThrows(JsonParseException.class, () -> gson.fromJson(json, PackageRef.class), json);
        }
    }

    @Test
    void supportsNullReferences() {
        assertNull(gson.fromJson("null", PackageRef.class));
        assertEquals("null", gson.toJson(null, PackageRef.class));
    }

    @Test
    void requiresParentOnlyForRelativeReferences() {
        assertThrows(IllegalArgumentException.class, () -> PackageRef.relative("X").toCanonicalPackage(null));
        assertEquals("X", PackageRef.absolute("X").toCanonicalPackage(null));
    }
}

package io.github.piscescup.fabricmc.carpetgui.adapter.codec;

import com.google.gson.*;
import io.github.piscescup.fabricmc.carpetgui.adapter.Accessor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccessorJsonCodecTest {
    private final Gson gson = new GsonBuilder()
        .registerTypeHierarchyAdapter(Accessor.class, new AccessorJsonCodec()).create();

    private Accessor<?> roundTrip(Accessor<?> accessor) {
        return gson.fromJson(gson.toJson(accessor), Accessor.class);
    }

    @Test void roundTripsAllFourMemberKinds() {
        Target instance = new Target();
        assertEquals("instance", roundTrip(Accessor.ofInstanceField("value")).get(instance));
        assertEquals("static", roundTrip(Accessor.ofStaticField("STATIC")).get(Target.class));
        assertEquals("instance", roundTrip(Accessor.ofInstanceMethod("value")).get(instance));
        assertEquals("static", roundTrip(Accessor.ofStaticMethod("staticValue")).get(Target.class));
    }

    @Test void preservesPrimitiveAndNullableMethodArguments() {
        var accessor = Accessor.ofStaticMethod("combine", new Class<?>[]{int.class, String.class}, new Object[]{7, null});
        assertEquals("7:null", roundTrip(accessor).get(Target.class));
    }

    @Test void preservesArrayArguments() {
        var accessor = Accessor.ofStaticMethod("sum", new Class<?>[]{int[].class}, new Object[]{new int[]{2, 3}});
        assertEquals(5, roundTrip(accessor).get(Target.class));
    }

    @Test void preservesCheckedWrapperAndStaticMode() {
        var restored = roundTrip(Accessor.<String>ofStaticField("STATIC").checked(String.class));
        assertTrue(restored.isStaticAccess());
        assertEquals("static", restored.get(Target.class));
        var invalid = roundTrip(Accessor.<Integer>ofStaticField("STATIC").checked(Integer.class));
        assertThrows(ClassCastException.class, () -> invalid.get(Target.class));
    }

    @Test void serializationDoesNotInvokeMembers() {
        var restored = roundTrip(Accessor.ofStaticMethod("fail"));
        assertThrows(IllegalStateException.class, () -> restored.get(Target.class));
    }

    @Test void rejectsInvalidDescriptions() {
        for (String json : new String[]{"[]", "{}", "{\"type\":\"UNKNOWN\",\"name\":\"x\"}",
            "{\"type\":\"STATIC_METHOD\",\"name\":\"x\",\"args\":[]}",
            "{\"type\":\"STATIC_METHOD\",\"name\":\"x\",\"parameterTypes\":[\"int\"],\"args\":[null]}",
            "{\"type\":\"CHECKED\",\"resultType\":\"java.lang.String\"}"}) {
            assertThrows(JsonParseException.class, () -> gson.fromJson(json, Accessor.class), json);
        }
    }

    @Test void rejectsCustomAccessorsWithoutDescriptions() {
        Accessor<Object> custom = new Accessor<>() {
            @Override public Object get(Object target) { throw new AssertionError("Must not invoke"); }
            @Override public boolean isStaticAccess() { return true; }
        };
        assertThrows(JsonParseException.class, () -> gson.toJson(custom));
    }

    @Test void roundTripsRegisteredExtensionAccess() {
        var restored = roundTrip(Accessor.ofRegisteredExtension());
        assertInstanceOf(Accessor.RegisteredExtensionAccess.class, restored);
        assertTrue(restored.isStaticAccess());
        assertEquals("REGISTERED_EXTENSION", gson.toJsonTree(restored, Accessor.class)
            .getAsJsonObject().get("type").getAsString());
        assertThrows(IllegalArgumentException.class, () -> restored.get(String.class));
        assertThrows(IllegalArgumentException.class, () -> restored.get(new Object()));
    }

    @Test void supportsNullAccessors() {
        assertNull(gson.fromJson("null", Accessor.class));
        assertEquals("null", gson.toJson(null, Accessor.class));
    }

    private static final class Target {
        private String value = "instance";
        private static final String STATIC = "static";
        private String value() { return value; }
        private static String staticValue() { return STATIC; }
        private static String combine(int number, String text) { return number + ":" + text; }
        private static int sum(int[] values) { return values[0] + values[1]; }
        private static void fail() { throw new AssertionError("Should only run on get()"); }
    }
}

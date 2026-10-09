package io.github.piscescup.fabricmc.carpetgui.adapter.codec;

import com.google.gson.*;
import io.github.piscescup.fabricmc.carpetgui.adapter.*;

import java.lang.reflect.Type;

/** Only persists builder inputs; runtime instances and caches are excluded. */
public final class CarpetAddonAdapterJsonCodec
    implements JsonSerializer<CarpetAddonAdapter>, JsonDeserializer<CarpetAddonAdapter> {
    @Override
    public JsonElement serialize(CarpetAddonAdapter source, Type type, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        json.addProperty("schemaVersion", 1);
        json.addProperty("modId", source.carpetModId());
        if (source.parentPackage() != null) json.addProperty("parentPackage", source.parentPackage());
        if (source.extensionClassRef() != null) {
            put(json, "extensionClass", source.extensionClassRef(), PackageRef.class, context);
            put(json, "extensionAccessor", source.extensionAccessor(), Accessor.class, context);
        }
        if (source.settingsClassRef() != null) {
            put(json, "settingsClass", source.settingsClassRef(), PackageRef.class, context);
            put(json, "settingsAccessor", source.settingsAccessor(), Accessor.class, context);
        }
        switch (source) {
            case RuleClassCarpetAddonAdapter rules -> {
                json.addProperty("type", "RULES");
                put(json, "ruleClass", rules.ruleClassRef(), PackageRef.class, context);
                put(json, "rulesAccessor", rules.rulesAccessor(), Accessor.class, context);
                put(json, "ruleNameAccessor", rules.ruleNameAccessor(), Accessor.class, context);
            }
            case AnnotationCarpetAddonAdapter annotation -> {
                json.addProperty("type", "ANNOTATION");
                put(json, "annotationClass", annotation.annotationClassRef(), PackageRef.class, context);
                put(json, "settingsClassesAccessor", annotation.settingsClassesAccessor(), Accessor.class, context);
            }
            case ManagerCarpetAddonAdapter manager -> {
                json.addProperty("type", "MANAGER");
                put(json, "managerClass", manager.managerClassRef(), PackageRef.class, context);
                put(json, "managerAccessor", manager.managerAccessor(), Accessor.class, context);
            }
            default -> throw new JsonParseException("Unsupported adapter: " + source.getClass().getName());
        }
        return json;
    }

    @Override
    public CarpetAddonAdapter deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
        if (!json.isJsonObject()) throw new JsonParseException("Adapter must be an object");
        JsonObject object = json.getAsJsonObject();
        JsonElement version = required(object, "schemaVersion");
        if (!(version instanceof JsonPrimitive primitive) || !primitive.isNumber()
            || !primitive.getAsBigDecimal().equals(java.math.BigDecimal.ONE)) {
            throw new JsonParseException("Unsupported adapter schemaVersion; expected 1");
        }
        try {
            String id = string(object, "modId");
            String parent = object.has("parentPackage") ? string(object, "parentPackage") : null;
            AdapterBuilder<?> builder;
            switch (string(object, "type")) {
                case "RULES" -> builder = CarpetAddonAdapter.ruleBuilder(id, parent)
                    .carpetRuleClassName(read(object, "ruleClass", PackageRef.class, context))
                    .rulesAccessor(accessor(object, "rulesAccessor", context))
                    .ruleNameAccessor(accessor(object, "ruleNameAccessor", context));
                case "ANNOTATION" -> {
                    AnnotationAdapterBuilder annotation = CarpetAddonAdapter.annotationBuilder(id, parent)
                        .ruleAnnotationClassName(read(object, "annotationClass", PackageRef.class, context));
                    if (present(object, "settingsClassesAccessor")) {
                        annotation.settingsClassesAccessor(accessor(object, "settingsClassesAccessor", context));
                    }
                    builder = annotation;
                }
                case "MANAGER" -> builder = CarpetAddonAdapter.managerBuilder(id, parent)
                    .managerClassName(read(object, "managerClass", PackageRef.class, context))
                    .managerAccessor(accessor(object, "managerAccessor", context));
                default -> throw new JsonParseException("Unknown adapter type: " + string(object, "type"));
            }
            if (present(object, "extensionClass")) builder.carpetExtensionClassName(read(object, "extensionClass", PackageRef.class, context));
            if (present(object, "extensionAccessor")) builder.extensionAccessor(accessor(object, "extensionAccessor", context));
            if (present(object, "settingsClass")) builder.settingsClassName(read(object, "settingsClass", PackageRef.class, context));
            if (present(object, "settingsAccessor")) builder.settingsAccessor(accessor(object, "settingsAccessor", context));
            CarpetAddonAdapter result = (CarpetAddonAdapter) builder.build();
            if (type instanceof Class<?> expected && !expected.isInstance(result)) {
                throw new JsonParseException("Adapter type does not match requested " + expected.getName());
            }
            return result;
        } catch (IllegalArgumentException | NullPointerException failure) {
            throw new JsonParseException("Invalid adapter configuration: " + failure.getMessage(), failure);
        }
    }

    private static void put(JsonObject json, String key, Object value, Class<?> type, JsonSerializationContext context) {
        if (value != null) json.add(key, context.serialize(value, type));
    }
    private static boolean present(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull();
    }
    private static JsonElement required(JsonObject json, String key) {
        if (!present(json, key)) throw new JsonParseException("Missing adapter field: " + key);
        return json.get(key);
    }
    private static String string(JsonObject json, String key) {
        JsonElement value = required(json, key);
        if (!(value instanceof JsonPrimitive text) || !text.isString() || text.getAsString().isBlank()) {
            throw new JsonParseException("Adapter." + key + " must be a non-blank string");
        }
        return text.getAsString();
    }
    private static <T> Accessor<T> accessor(JsonObject json, String key, JsonDeserializationContext context) {
        return context.deserialize(required(json, key), Accessor.class);
    }
    private static <T> T read(JsonObject json, String key, Class<T> type, JsonDeserializationContext context) {
        return context.deserialize(required(json, key), type);
    }
}

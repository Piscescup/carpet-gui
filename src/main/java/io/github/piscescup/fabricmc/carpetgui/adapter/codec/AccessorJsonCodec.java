package io.github.piscescup.fabricmc.carpetgui.adapter.codec;

import com.google.gson.*;
import io.github.piscescup.fabricmc.carpetgui.adapter.Accessor;

import java.lang.reflect.Type;

/** Serializes configured member access, never its target or the result of invoking it. */
public final class AccessorJsonCodec implements JsonSerializer<Accessor<?>>, JsonDeserializer<Accessor<?>> {
    @Override
    public JsonElement serialize(Accessor<?> source, Type type, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        switch (source) {
            case Accessor.RegisteredExtensionAccess ignored -> json.addProperty("type", "REGISTERED_EXTENSION");
            case Accessor.CheckedAccess<?>(Accessor<?> source1, Class<?> resultType) -> {
                json.addProperty("type", "CHECKED");
                json.addProperty("resultType", resultType.getName());
                json.add("source", context.serialize(source1, Accessor.class));
            }
            case Accessor.FieldAccess<?>(String name, boolean isStaticAccess) -> {
                json.addProperty("type", isStaticAccess ? "STATIC_FIELD" : "INSTANCE_FIELD");
                json.addProperty("name", name);
            }
            case Accessor.MethodAccess<?> method -> {
                json.addProperty("type", method.isStaticAccess() ? "STATIC_METHOD" : "INSTANCE_METHOD");
                json.addProperty("name", method.name());
                Class<?>[] types = method.parameterTypes();
                Object[] values = method.args();
                if (types.length != 0) {
                    JsonArray parameters = new JsonArray();
                    JsonArray arguments = new JsonArray();
                    for (int i = 0; i < types.length; i++) {
                        parameters.add(types[i].getName());
                        arguments.add(context.serialize(values[i], types[i]));
                    }
                    json.add("parameterTypes", parameters);
                    json.add("args", arguments);
                }
            }
            default ->
                throw new JsonParseException("Accessor has no serializable member description: " + source.getClass()
                    .getName());
        }
        return json;
    }

    @Override
    public Accessor<?> deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
        if (!json.isJsonObject()) throw new JsonParseException("Accessor must be an object");
        JsonObject object = json.getAsJsonObject();
        String kind = string(object, "type");
        if (kind.equals("REGISTERED_EXTENSION")) {
            return Accessor.ofRegisteredExtension();
        }
        if (kind.equals("CHECKED")) {
            Accessor<?> source = context.deserialize(object.get("source"), Accessor.class);
            if (source == null) throw new JsonParseException("CHECKED accessor requires source");
            return checked(source, classForName(string(object, "resultType")));
        }
        String name = string(object, "name");
        return switch (kind) {
            case "STATIC_FIELD" -> field(object, name, true);
            case "INSTANCE_FIELD" -> field(object, name, false);
            case "STATIC_METHOD", "INSTANCE_METHOD" -> method(object, name, kind.equals("STATIC_METHOD"), context);
            default -> throw new JsonParseException("Unknown accessor type: " + kind);
        };
    }

    private static Accessor<?> field(JsonObject object, String name, boolean staticAccess) {
        if (object.has("parameterTypes") || object.has("args")) {
            throw new JsonParseException("Field accessors cannot have method arguments");
        }
        return staticAccess ? Accessor.ofStaticField(name) : Accessor.ofInstanceField(name);
    }

    private static Accessor<?> method(JsonObject object, String name, boolean staticAccess,
        JsonDeserializationContext context) {
        boolean hasTypes = object.has("parameterTypes");
        boolean hasArgs = object.has("args");
        if (hasTypes != hasArgs) throw new JsonParseException("parameterTypes and args must be provided together");
        if (!hasTypes) return staticAccess ? Accessor.ofStaticMethod(name) : Accessor.ofInstanceMethod(name);
        if (!object.get("parameterTypes").isJsonArray() || !object.get("args").isJsonArray()) {
            throw new JsonParseException("parameterTypes and args must be arrays");
        }
        JsonArray parameters = object.getAsJsonArray("parameterTypes");
        JsonArray arguments = object.getAsJsonArray("args");
        if (parameters.size() != arguments.size()) throw new JsonParseException("Method argument count mismatch");
        Class<?>[] types = new Class<?>[parameters.size()];
        Object[] values = new Object[parameters.size()];
        for (int i = 0; i < types.length; i++) {
            JsonElement parameter = parameters.get(i);
            if (!(parameter instanceof JsonPrimitive text) || !text.isString()) {
                throw new JsonParseException("Parameter type must be a class name string");
            }
            types[i] = classForName(text.getAsString());
            if (types[i] == void.class) throw new JsonParseException("void is not a parameter type");
            if (types[i].isPrimitive() && arguments.get(i).isJsonNull()) {
                throw new JsonParseException("Primitive argument cannot be null at index " + i);
            }
            values[i] = context.deserialize(arguments.get(i), types[i]);
        }
        return staticAccess ? Accessor.ofStaticMethod(name, types, values) : Accessor.ofInstanceMethod(name, types, values);
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (!(value instanceof JsonPrimitive text) || !text.isString() || text.getAsString().isBlank()) {
            throw new JsonParseException("Accessor." + key + " must be a non-blank string");
        }
        return text.getAsString();
    }

    private static Class<?> classForName(String name) {
        try {
            return switch (name) {
                case "boolean" -> boolean.class;
                case "byte" -> byte.class;
                case "short" -> short.class;
                case "int" -> int.class;
                case "long" -> long.class;
                case "float" -> float.class;
                case "double" -> double.class;
                case "char" -> char.class;
                case "void" -> void.class;
                default -> Class.forName(name, false, AccessorJsonCodec.class.getClassLoader());
            };
        } catch (ClassNotFoundException | LinkageError failure) {
            throw new JsonParseException("Cannot load accessor type " + name, failure);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Accessor<?> checked(Accessor<?> source, Class<?> type) {
        return ((Accessor) source).checked(type);
    }
}

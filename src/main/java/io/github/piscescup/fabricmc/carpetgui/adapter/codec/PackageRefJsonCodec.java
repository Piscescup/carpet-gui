/*
 * This file is part of the Carpet GUI project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  PiscesCup and contributors
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

package io.github.piscescup.fabricmc.carpetgui.adapter.codec;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import io.github.piscescup.fabricmc.carpetgui.adapter.PackageRef;

import java.lang.reflect.Type;

/**
 * Gson codec that stores a {@link PackageRef}'s original class name and resolution mode without
 * loading or resolving the referenced class.
 *
 * <pre>{@code
 * Gson gson = new GsonBuilder()
 *     .registerTypeAdapter(PackageRef.class, new PackageRefJsonCodec())
 *     .create();
 * String json = gson.toJson(PackageRef.relative("MySettings"), PackageRef.class);
 * }</pre>
 *
 * @see PackageRef
 * @see CarpetAddonAdapterJsonCodec
 */
public final class PackageRefJsonCodec implements JsonSerializer<PackageRef>, JsonDeserializer<PackageRef> {
    /**
     * Serializes the stored name and absolute-mode flag.
     *
     * <pre>{@code
     * JsonElement json = codec.serialize(ref, PackageRef.class, context);
     * }</pre>
     *
     * @param source the package reference to serialize
     * @param type the declared Gson type
     * @param context the current Gson serialization context
     * @return a JSON object containing {@code name} and {@code absolute}
     * @throws NullPointerException if {@code source} is {@code null}
     */
    @Override
    public JsonElement serialize(PackageRef source, Type type, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        json.addProperty("name", source.name());
        json.addProperty("absolute", source.absolute());
        return json;
    }

    /**
     * Reconstructs a package reference from its stored name and resolution mode.
     *
     * <pre>{@code
     * PackageRef ref = codec.deserialize(json, PackageRef.class, context);
     * }</pre>
     *
     * @param json the package-reference JSON object
     * @param type the requested Gson type
     * @param context the current Gson deserialization context
     * @return the reconstructed absolute or relative reference
     * @throws JsonParseException if the input is not an object or its fields have invalid types
     */
    @Override
    public PackageRef deserialize(JsonElement json, Type type, JsonDeserializationContext context)
        throws JsonParseException
    {
        if (!json.isJsonObject()) throw new JsonParseException("PackageRef must be an object");

        JsonObject object = json.getAsJsonObject();
        JsonElement name = object.get("name");
        JsonElement absolute = object.get("absolute");

        if (!(name instanceof JsonPrimitive text) || !text.isString() || text.getAsString().isBlank()) {
            throw new JsonParseException("PackageRef.name must be a non-blank string");
        }
        if (!(absolute instanceof JsonPrimitive mode) || !mode.isBoolean()) {
            throw new JsonParseException("PackageRef.absolute must be a boolean");
        }
        return mode.getAsBoolean() ?
            PackageRef.absolute(text.getAsString()) :
            PackageRef.relative(text.getAsString());
    }
}

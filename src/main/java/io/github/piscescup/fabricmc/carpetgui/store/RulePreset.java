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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;

/** Portable values only: no local history, author identity, server address or push status. */
public record RulePreset(String name, String tag, Map<String, String> rules) {
    public static final int MAX_BYTES = 1_048_576;
    public static final int MAX_RULES = 2048;

    public RulePreset {
        if (name == null || name.isBlank() || name.length() > 64 || tag == null || tag.length() > 32
            || rules == null || rules.isEmpty() || rules.size() > MAX_RULES) throw invalid();
        if (name.chars().anyMatch(Character::isISOControl) || tag.chars().anyMatch(Character::isISOControl)) throw invalid();
        var identities = new HashSet<String>();
        rules.forEach((key, value) -> {
            if (key == null || key.isBlank() || key.length() > 512 || value == null || value.length() > 4096
                || key.chars().anyMatch(Character::isISOControl) || value.chars().anyMatch(Character::isISOControl)) throw invalid();
            String identity = key.contains("::") ? key.substring(key.indexOf("::") + 2) : key;
            if (identity.isBlank() || !identities.add(identity)) throw invalid();
        });
        rules = Collections.unmodifiableMap(new LinkedHashMap<>(rules));
    }

    public static RulePreset decode(String text) {
        if (text == null || text.length() > MAX_BYTES || text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_BYTES) throw invalid();
        try (var reader = new JsonReader(new StringReader(text))) {
            var fields = new HashSet<String>();
            String format = null, name = null, tag = "";
            int version = 0;
            Map<String, String> rules = null;
            reader.beginObject();
            while (reader.hasNext()) {
                String field = reader.nextName();
                if (!fields.add(field)) throw invalid();
                switch (field) {
                    case "format" -> format = string(reader);
                    case "version" -> { if (reader.peek() != JsonToken.NUMBER) throw invalid(); version = reader.nextInt(); }
                    case "name" -> name = string(reader);
                    case "tag" -> tag = string(reader);
                    case "rules" -> {
                        rules = new LinkedHashMap<>();
                        reader.beginObject();
                        while (reader.hasNext()) {
                            String key = reader.nextName();
                            if (rules.containsKey(key) || rules.size() >= MAX_RULES) throw invalid();
                            rules.put(key, string(reader));
                        }
                        reader.endObject();
                    }
                    default -> throw invalid();
                }
            }
            reader.endObject();
            if (reader.peek() != JsonToken.END_DOCUMENT || !"carpet-gui-preset".equals(format) || version != 1) throw invalid();
            return new RulePreset(name, tag, rules);
        } catch (IOException | IllegalStateException | NumberFormatException failure) {
            throw new IllegalArgumentException("Invalid Carpet GUI preset", failure);
        }
    }

    private static String string(JsonReader reader) throws IOException {
        if (reader.peek() != JsonToken.STRING) throw invalid();
        return reader.nextString();
    }

    public String encode() {
        var json = new JsonObject();
        json.addProperty("format", "carpet-gui-preset");
        json.addProperty("version", 1);
        json.addProperty("name", name);
        json.addProperty("tag", tag);
        var values = new JsonObject();
        rules.forEach(values::addProperty);
        json.add("rules", values);
        String text = new GsonBuilder().setPrettyPrinting().create().toJson(json);
        if (text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_BYTES) throw invalid();
        return text;
    }

    private static IllegalArgumentException invalid() { return new IllegalArgumentException("Invalid Carpet GUI preset"); }
}

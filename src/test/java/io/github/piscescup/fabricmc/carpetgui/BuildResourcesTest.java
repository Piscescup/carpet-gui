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

package io.github.piscescup.fabricmc.carpetgui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class BuildResourcesTest {
    private JsonObject resource(String path) throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
    }

    @Test
    void mixinCompatibilityMatchesDeclaredJavaVersion() throws Exception {
        String java = resource("fabric.mod.json").getAsJsonObject("depends").get("java").getAsString().replaceAll("\\D", "");
        assertEquals("JAVA_" + java, resource("carpet-gui.mixins.json").get("compatibilityLevel").getAsString());
    }

    @Test
    void workspaceTranslationsHaveMatchingKeysAndUnrecordedText() throws Exception {
        var en = resource("assets/carpet-gui/lang/en_us.json");
        var zh = resource("assets/carpet-gui/lang/zh_cn.json");
        assertEquals("未记录", zh.get("carpet-gui.workspace.unrecorded").getAsString());
        for (String key : en.keySet()) if (key.startsWith("carpet-gui.workspace.")) assertTrue(zh.has(key), key);
        for (String key : zh.keySet()) if (key.startsWith("carpet-gui.workspace.")) assertTrue(en.has(key), key);
    }
}

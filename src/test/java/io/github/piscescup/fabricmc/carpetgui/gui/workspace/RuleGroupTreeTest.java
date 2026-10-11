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

package io.github.piscescup.fabricmc.carpetgui.gui.workspace;

import com.google.gson.Gson;
import io.github.piscescup.fabricmc.carpetgui.store.RuleGroupStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleGroupTreeTest {
    private static RuleGroupStore.Group group(String name) {
        return new Gson().fromJson("{\"id\":\"id\",\"name\":\"" + name + "\"}", RuleGroupStore.Group.class);
    }

    @Test
    void originIsTheSingleRootAndSlashNamesCreateDirectories() {
        var root = RuleGroupTree.of(List.of(group("root-rule"), group("pry/test"), group("pry/other"))).getFirst();
        assertEquals("origin", root.name());
        assertEquals(1, root.children().size());
        assertEquals("pry", root.children().getFirst().name());
        assertEquals("origin/pry", root.children().getFirst().path());
        assertEquals("root-rule", root.groups().getFirst().name());
        assertEquals(List.of("other", "test"), root.children().getFirst().groups().stream().map(RuleGroupTree.Entry::name).toList());
    }

    @Test
    void emptyAndWhitespacePathSegmentsDoNotCreateFakeFolders() {
        var root = RuleGroupTree.of(List.of(group(" / pry // test / "))).getFirst();
        assertEquals("pry", root.children().getFirst().name());
        assertEquals("test", root.children().getFirst().groups().getFirst().name());
    }
}

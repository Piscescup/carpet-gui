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

package io.github.piscescup.fabricmc.carpetgui.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleConfigurationStateTest {
    @Test
    void savingSameValueStillChangesExplicitConfigurationState() {
        var before = new RuleConfigurationSnapshot.Entry("carpet", "rule", "true", false);
        var after = ServerRuleChanges.configurationEntry("carpet", "rule", "true", before, null, true, "true");
        assertTrue(after.explicitlyConfigured());
        assertEquals("true", after.value());
        assertNotEquals(before, after);
    }

    @Test
    void removingSameValueStillClearsExplicitConfigurationState() {
        var before = new RuleConfigurationSnapshot.Entry("carpet", "rule", "false", true);
        var after = ServerRuleChanges.configurationEntry("carpet", "rule", "false", before, "false", false, null);
        assertFalse(after.explicitlyConfigured());
        assertEquals("false", after.value());
        assertNotEquals(before, after);
    }

    @Test
    void pollingUnchangedFileDoesNotReplaceSavedBaselineWithTemporaryValue() {
        var before = new RuleConfigurationSnapshot.Entry("carpet", "rule", "false", true);
        var after = ServerRuleChanges.configurationEntry("carpet", "rule", "true", before, "false", true, "false");
        assertEquals(before, after);
    }
}

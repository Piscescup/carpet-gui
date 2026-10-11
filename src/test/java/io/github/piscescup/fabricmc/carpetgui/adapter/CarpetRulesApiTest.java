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

package io.github.piscescup.fabricmc.carpetgui.adapter;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.SettingsManager;
import io.github.piscescup.fabricmc.carpetgui.api.CarpetModRulesApi;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CarpetRulesApiTest {
    private static SettingsManager manager(String id) {
        return new SettingsManager("test", id, id);
    }

    private static CarpetRule<?> rule(SettingsManager manager, String name) {
        return (CarpetRule<?>) Proxy.newProxyInstance(
                CarpetRule.class.getClassLoader(), new Class<?>[]{CarpetRule.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "name", "toString" -> name;
                    case "settingsManager" -> manager;
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    @Test
    void providerOnlyNeedsIdentityAndLiveRulesAndCanUseMultipleManagers() {
        CarpetRule<?> first = rule(manager("first"), "firstRule");
        CarpetRule<?> second = rule(manager("second"), "secondRule");
        first.settingsManager().addCarpetRule(first);
        second.settingsManager().addCarpetRule(second);
        CarpetModRulesApi provider = new CarpetModRulesApi() {
            @Override
            public String carpetModId() { return "test_addon"; }

            @Override
            public String carpetFancyName() { return "Test Addon"; }

            @Override
            public Collection<CarpetRule<?>> asCarpetRules() { return List.of(first, second); }
        };
        assertEquals(List.of(first, second), List.copyOf(provider.asCarpetRules()));
        assertTrue(provider.getSettingsManagerIds().isEmpty());
    }

    @Test
    void adapterResolvesLiveRulesInOrderAndSkipsMissingAndDuplicateNames() {
        TestAdapter adapter = new TestAdapter();
        adapter.current = manager("test");
        CarpetRule<?> first = rule(adapter.current, "first");
        CarpetRule<?> second = rule(adapter.current, "second");
        adapter.current.addCarpetRule(first);
        adapter.current.addCarpetRule(second);
        // A foreign-manager instance must not be exposed even if placed in this manager's map.
        adapter.current.addCarpetRule(rule(manager("other"), "foreign"));
        adapter.names = Arrays.asList("second", null, "", " ", "missing", "first", "second", "foreign");

        Collection<CarpetRule<?>> resolved = adapter.asCarpetRules();
        assertEquals(List.of(second, first), List.copyOf(resolved));
        assertSame(second, resolved.iterator().next());
        assertThrows(UnsupportedOperationException.class, resolved::clear);
    }

    @Test
    void unavailableManagerIsRetriedWithoutDiscoveringNamesPrematurely() {
        TestAdapter adapter = new TestAdapter();
        assertTrue(adapter.asCarpetRules().isEmpty());
        assertEquals(0, adapter.nameReads);
        adapter.current = manager("ready");
        CarpetRule<?> live = rule(adapter.current, "readyRule");
        adapter.current.addCarpetRule(live);
        adapter.names = List.of("readyRule");
        assertSame(live, adapter.asCarpetRules().iterator().next());
    }

    @Test
    void discoveryFailuresProduceEmptyRulesAndCanRecover() {
        TestAdapter adapter = new TestAdapter();
        adapter.current = manager("test");
        adapter.failNames = true;
        assertTrue(adapter.asCarpetRules().isEmpty());
        assertTrue(adapter.asCarpetRules().isEmpty());
        assertEquals(1, adapter.warnings.size());
        adapter.failNames = false;
        adapter.names = null;
        assertTrue(adapter.asCarpetRules().isEmpty());
        CarpetRule<?> live = rule(adapter.current, "recovered");
        adapter.current.addCarpetRule(live);
        adapter.names = List.of("recovered");
        assertSame(live, adapter.asCarpetRules().iterator().next());
    }

    @Test
    void managerLookupFailuresAreContainedByAdapter() {
        TestAdapter adapter = new TestAdapter();
        adapter.failManager = true;
        assertTrue(adapter.asCarpetRules().isEmpty());
        assertTrue(adapter.warnings.contains("rules"));
    }

    @Test
    void adapterWithoutExtensionUsesGlobalManagerAndUnreadyExtensionDoesNot() {
        CarpetAddonAdapter adapter = new AnnotationBuilder("test_addon", null)
                .settingsClassName(PackageRef.absolute("example.Settings"))
                .onCarpetRuleAnnotation()
                .build();
        assertSame(CarpetServer.settingsManager, adapter.settingsManager());
        assertNull(adapter.manager(null));
    }

    @Test
    void extensionManagerIsRereadAfterInitializationAndReplacement() {
        TestAdapter adapter = new TestAdapter();
        MutableExtension extension = new MutableExtension();
        assertSame(CarpetServer.settingsManager, adapter.manager(extension));
        extension.current = manager("initialized");
        assertSame(extension.current, adapter.manager(extension));
        extension.current = manager("replacement");
        assertSame(extension.current, adapter.manager(extension));
    }

    private static final class MutableExtension implements CarpetExtension {
        private SettingsManager current;

        @Override
        public SettingsManager extensionSettingsManager() { return current; }
    }

    private static final class TestAdapter extends CarpetAddonAdapter {
        private SettingsManager current;
        private Collection<String> names = List.of();
        private int nameReads;
        private boolean failNames;
        private boolean failManager;

        private TestAdapter() { super(new AnnotationBuilder("test_addon", null)); }

        @Override
        public SettingsManager settingsManager() {
            if (failManager) throw new IllegalStateException("Manager unavailable");
            return current;
        }

        @Override
        protected Collection<String> getRuleNames(Object... args) throws ClassNotFoundException {
            nameReads++;
            if (failNames) throw new ClassNotFoundException("Optional settings unavailable");
            return names;
        }

        @Override
        protected void warn(String source, Throwable failure) {
            // Avoid requiring a running Fabric environment just to record test diagnostics.
            warnings.add(source);
        }
    }
}

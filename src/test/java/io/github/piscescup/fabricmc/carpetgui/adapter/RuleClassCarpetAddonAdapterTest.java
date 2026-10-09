package io.github.piscescup.fabricmc.carpetgui.adapter;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleClassCarpetAddonAdapterTest {
    @Test
    void readsStaticRulesAndFiltersDuplicateAndEmptyNames() throws ReflectiveOperationException {
        var adapter = build(Accessor.ofStaticField("RULES"), Accessor.ofInstanceMethod("name"));
        assertEquals(List.of("first", "second"), adapter.getRuleNames());
    }

    @Test
    void readsInstanceRulesFromConfiguredSettingsAccessor() throws ReflectiveOperationException {
        var adapter = build(Accessor.ofInstanceField("rules"), Accessor.ofInstanceField("name"));
        assertEquals(List.of("live"), adapter.getRuleNames());
    }

    @Test
    void retriesSettingsAfterNullInsteadOfCreatingAnInstance() throws ReflectiveOperationException {
        var builder = builder(Accessor.ofInstanceField("rules"), Accessor.ofInstanceMethod("name"));
        builder.settingsAccessor(new Accessor<Object>() {
            private int calls;
            @Override public boolean isStaticAccess() { return true; }
            @Override public Object get(Object target) { return calls++ == 0 ? null : Settings.SETTINGS; }
        });
        var adapter = (RuleClassCarpetAddonAdapter) builder.build();
        assertEquals(List.of(), adapter.getRuleNames());
        assertEquals(List.of("live"), adapter.getRuleNames());
    }

    @Test
    void rejectsWrongCollectionAndRuleTypes() {
        assertThrows(IllegalArgumentException.class,
            () -> build(Accessor.ofStaticField("NOT_COLLECTION"), Accessor.ofInstanceMethod("name")).getRuleNames());
        assertThrows(IllegalArgumentException.class,
            () -> build(Accessor.ofStaticField("WRONG_RULES"), Accessor.ofInstanceMethod("name")).getRuleNames());
    }

    @Test
    void rejectsWrongNameTypeEvenWhenAccessorGenericTypeClaimsString() {
        assertThrows(IllegalArgumentException.class,
            () -> build(Accessor.ofStaticField("RULES"), Accessor.<String>ofInstanceMethod("number")).getRuleNames());
    }

    @Test
    void validatesRequiredConfigurationWithoutLoadingAddonClasses() {
        assertThrows(NullPointerException.class,
            () -> CarpetAddonAdapter.ruleBuilder("test-addon", "unused")
                .settingsClassName("MissingSettings")
                .carpetRuleClassName("MissingRule")
                .build());
    }

    private static RuleClassCarpetAddonAdapter build(Accessor<?> rules, Accessor<String> names) {
        return (RuleClassCarpetAddonAdapter) builder(rules, names).build();
    }

    private static RuleAdapterBuilder builder(Accessor<?> rules, Accessor<String> names) {
        return CarpetAddonAdapter.ruleBuilder("test-addon", "unused")
            .settingsClassName(PackageRef.absolute(Settings.class.getName()))
            .carpetRuleClassName(PackageRef.absolute(Rule.class.getName()))
            .rulesAccessor(rules)
            .ruleNameAccessor(names);
    }

    private static final class Settings {
        private static final Settings SETTINGS = new Settings();
        private static final List<Rule> RULES = Arrays.asList(
            new Rule("first"), null, new Rule(""), new Rule("first"), new Rule("second"));
        private static final String NOT_COLLECTION = "invalid";
        private static final List<String> WRONG_RULES = List.of("invalid");
        private final List<Rule> rules = List.of(new Rule("live"));
    }

    private record Rule(String name) {
        private int number() { return 1; }
    }
}

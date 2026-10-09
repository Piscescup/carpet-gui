package io.github.piscescup.fabricmc.carpetgui.adapter;

import io.github.piscescup.fabricmc.carpetgui.integration.carpet.CarpetSeriesMods;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AnnotationCarpetAddonAdapterTest {
    @Test
    void scansSettingsAndNestedClasses() throws ClassNotFoundException {
        var adapter = (AnnotationCarpetAddonAdapter) builder(Settings.class).build();
        assertEquals(Set.of("direct", "nested"), Set.copyOf(adapter.getRuleNames()));
    }

    @Test
    void scansClassesInAnIndexInsteadOfTheIndexHolder() throws ClassNotFoundException {
        var adapter = (AnnotationCarpetAddonAdapter) builder(Index.class)
            .settingsClassesAccessor(Accessor.ofStaticField("ruleClasses")).build();
        assertEquals(List.of("indexed"), List.copyOf(adapter.getRuleNames()));
    }

    @Test
    void rejectsInvalidIndexEntries() {
        var adapter = (AnnotationCarpetAddonAdapter) builder(Index.class)
            .settingsClassesAccessor(Accessor.ofStaticField("invalidClasses")).build();
        assertThrows(IllegalArgumentException.class, adapter::getRuleNames);
    }

    @Test
    void buildsAllCompatibilityDeclarationsWithoutLoadingOptionalAddonClasses() {
        var ids = CarpetSeriesMods.CARPET_SERIES.stream().map(provider -> provider.carpetModId()).toList();
        assertEquals(Set.of("carpet-pry-addition", "carpet-igny-addition", "carpet-tis-addition",
            "carpet-org-addition", "carpet-ams-addition", "carpet-extra-extras", "gca",
            "carpet-rof-addition", "cca"), Set.copyOf(ids));
        assertEquals(9, ids.size());
    }

    private static AnnotationAdapterBuilder builder(Class<?> settings) {
        return CarpetAddonAdapter.annotationBuilder("test-addon", "unused")
            .settingsClassName(PackageRef.absolute(settings.getName()))
            .ruleAnnotationClassName(PackageRef.absolute(Marker.class.getName()));
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface Marker {}

    private static final class Settings {
        @Marker private static boolean direct;
        private static final class Nested {
            @Marker private static boolean nested;
        }
    }

    private static final class Index {
        @Marker private static boolean notARule;
        private static final List<Class<?>> ruleClasses = List.of(Indexed.class);
        private static final List<String> invalidClasses = List.of("not a class");
    }

    private static final class Indexed {
        @Marker private static boolean indexed;
    }
}

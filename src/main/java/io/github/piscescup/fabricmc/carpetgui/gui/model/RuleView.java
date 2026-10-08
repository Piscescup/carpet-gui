package io.github.piscescup.fabricmc.carpetgui.gui.model;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

/**
 * Read-only presentation contract; no dependency on a particular Carpet API.
 */
public interface RuleView {
    String id();

    /** Stable presentation identity; may include an owner/manager when local rule IDs repeat. */
    default String stateId() { return id(); }

    Component label();

    Component description();

    String value();

    String defaultValue();

    /** Effective startup value supplied by the current server configuration, when available. */
    default Optional<String> configuredValue() { return Optional.empty(); }

    /** Whether this rule is explicitly present in the current world's Carpet configuration. */
    default boolean explicitlyConfigured() { return false; }

    List<String> categories();

    default List<Component> extraInfo() { return List.of(); }

    /** Searchable text independent of the active GUI language. Backends may add other translations. */
    default List<String> searchTerms() {
        return List.of(id(), label().getString(), description().getString(), String.join(" ", categories()));
    }

    default boolean modified() {
        return !value().equals(defaultValue());
    }

    /** Current value differs from the value declared by the rule implementation. */
    default boolean differsFromInitialValue() {
        return modified();
    }

    /** Current value differs from the effective configuration loaded by the server. */
    default boolean differsFromConfiguredValue() {
        return configuredValue().map(configured -> !value().equals(configured)).orElse(false);
    }
}

package io.github.piscescup.fabricmc.carpetgui.gui.model;

import net.minecraft.network.chat.Component;

import java.util.List;

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

    List<String> categories();

    default List<Component> extraInfo() { return List.of(); }

    /** Searchable text independent of the active GUI language. Backends may add other translations. */
    default List<String> searchTerms() {
        return List.of(id(), label().getString(), description().getString(), String.join(" ", categories()));
    }

    default boolean modified() {
        return !value().equals(defaultValue());
    }
}

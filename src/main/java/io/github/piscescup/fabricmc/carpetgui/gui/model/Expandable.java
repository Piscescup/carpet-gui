package io.github.piscescup.fabricmc.carpetgui.gui.model;

/**
 * Shared by an individual [+]/[-] toggle and bulk expansion actions.
 */
public interface Expandable {
    boolean isExpanded();

    void setExpanded(boolean expanded);

    default void toggle() {
        setExpanded(!isExpanded());
    }

    static void setAll(Iterable<? extends Expandable> sections, boolean expanded) {
        for (Expandable section : sections) section.setExpanded(expanded);
    }
}

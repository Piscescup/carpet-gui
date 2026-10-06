package io.github.piscescup.fabricmc.carpetgui.gui.model;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Generic section state and animation; independent of Minecraft rendering and rule data.
 */
public final class CollapsibleSection<T>
    implements Expandable
{
    private static final double ANIMATION_SECONDS = 0.16;
    private final String id;
    private final List<T> items;
    private final Consumer<Boolean> onChange;
    private boolean expanded;
    private double progress;

    public CollapsibleSection(String id, List<T> items, boolean initiallyExpanded, Consumer<Boolean> onChange) {
        this.id = Objects.requireNonNull(id);
        this.items = List.copyOf(items);
        this.onChange = Objects.requireNonNull(onChange);
        expanded = initiallyExpanded;
        progress = expanded ? 1 : 0;
    }

    public String id() {
        return id;
    }

    public List<T> items() {
        return items;
    }

    @Override
    public boolean isExpanded() {
        return expanded;
    }

    @Override
    public void setExpanded(boolean expanded) {
        if (this.expanded == expanded) return;
        this.expanded = expanded;
        onChange.accept(expanded);
    }

    public void advance(double seconds) {
        double step = Math.max(0, seconds) / ANIMATION_SECONDS;
        progress = Math.clamp(progress + (expanded ? step : -step), 0, 1);
    }

    public double openFraction() {
        return progress * progress * (3 - 2 * progress);
    }

    public int bodyHeight(int rowHeight) {
        return (int) Math.round(items.size() * rowHeight * openFraction());
    }
}

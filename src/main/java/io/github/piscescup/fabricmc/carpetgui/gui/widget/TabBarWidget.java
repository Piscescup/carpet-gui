package io.github.piscescup.fabricmc.carpetgui.gui.widget;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.piscescup.fabricmc.carpetgui.gui.layout.GuiBounds;
import io.github.piscescup.fabricmc.carpetgui.gui.model.DropdownOption;
import io.github.piscescup.fabricmc.carpetgui.gui.render.GuiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.function.Consumer;

/** Reusable horizontal tabs with stable IDs, arrows, wheel scrolling and keyboard selection. */
public final class TabBarWidget<T extends DropdownOption> extends AbstractWidget {
    private static final int ARROW = 18;
    private static final int GAP = 4;
    private final List<? extends T> options;
    private final Consumer<T> selected;
    private int selectedIndex;
    private double scroll;

    public TabBarWidget(GuiBounds bounds, List<? extends T> options, String selectedId, Consumer<T> selected) {
        super(bounds.left(), bounds.top(), bounds.width(), bounds.height(), Component.empty());
        this.options = List.copyOf(options);
        this.selected = selected;
        for (int index = 0; index < options.size(); index++) {
            if (options.get(index).id().equals(selectedId)) selectedIndex = index;
        }
        if (!options.isEmpty()) setMessage(options.get(selectedIndex).label());
        revealSelection();
    }

    private int tabWidth(T option) {
        return Math.clamp(Minecraft.getInstance().font.width(option.label()) + 20, 50, Math.max(50, getWidth() - ARROW * 2 - GAP * 2));
    }

    private int contentWidth() {
        return options.stream().mapToInt(this::tabWidth).sum() + Math.max(0, options.size() - 1) * GAP;
    }

    private boolean overflowing() { return contentWidth() > getWidth(); }
    private int viewportLeft() { return getX() + (overflowing() ? ARROW + GAP : 0); }
    private int viewportWidth() { return Math.max(1, getWidth() - (overflowing() ? 2 * (ARROW + GAP) : 0)); }
    private double maxScroll() { return Math.max(0, contentWidth() - viewportWidth()); }

    private void revealSelection() {
        int left = 0;
        for (int index = 0; index < selectedIndex; index++) left += tabWidth(options.get(index)) + GAP;
        int right = options.isEmpty() ? left : left + tabWidth(options.get(selectedIndex));
        if (left < scroll) scroll = left;
        if (right > scroll + viewportWidth()) scroll = right - viewportWidth();
        scroll = Math.clamp(scroll, 0, maxScroll());
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        scroll = Math.clamp(scroll, 0, maxScroll());
        int left = viewportLeft();
        int right = left + viewportWidth();
        var font = Minecraft.getInstance().font;
        graphics.enableScissor(left, getY(), right, getY() + getHeight());
        try {
            int x = left - (int) Math.round(scroll);
            for (int index = 0; index < options.size(); index++) {
                T option = options.get(index);
                int width = tabWidth(option);
                boolean hovered = mouseX >= Math.max(left, x) && mouseX < Math.min(right, x + width)
                    && mouseY >= getY() && mouseY < getY() + getHeight();
                if (x < right && x + width > left) {
                    GuiTheme.button(graphics, x, getY(), width, getHeight(), hovered || isFocused() && index == selectedIndex,
                        index == selectedIndex);
                    graphics.centeredText(font, GuiTheme.fit(font, option.label().getString(), width - 10),
                        x + width / 2, getY() + (getHeight() - font.lineHeight) / 2, GuiTheme.TEXT);
                    if (hovered) graphics.setTooltipForNextFrame(option.label(), mouseX, mouseY);
                }
                x += width + GAP;
            }
        } finally { graphics.disableScissor(); }
        if (overflowing()) {
            arrow(graphics, getX(), "<", scroll > 0, mouseX, mouseY);
            arrow(graphics, getRight() - ARROW, ">", scroll < maxScroll(), mouseX, mouseY);
        }
    }

    private void arrow(GuiGraphicsExtractor graphics, int x, String label, boolean active, int mouseX, int mouseY) {
        boolean hovered = new GuiBounds(x, getY(), ARROW, getHeight()).contains(mouseX, mouseY);
        GuiTheme.button(graphics, x, getY(), ARROW, getHeight(), active && hovered, !active);
        var font = Minecraft.getInstance().font;
        graphics.centeredText(font, label, x + ARROW / 2, getY() + (getHeight() - font.lineHeight) / 2,
            active ? GuiTheme.TEXT : GuiTheme.MUTED);
    }

    @Override
    public void onClick(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (overflowing() && event.x() < viewportLeft()) {
            scroll = Math.clamp(scroll - viewportWidth() * 0.75, 0, maxScroll());
            return;
        }
        if (overflowing() && event.x() >= viewportLeft() + viewportWidth()) {
            scroll = Math.clamp(scroll + viewportWidth() * 0.75, 0, maxScroll());
            return;
        }
        double x = event.x() - viewportLeft() + scroll;
        int left = 0;
        for (int index = 0; index < options.size(); index++) {
            int width = tabWidth(options.get(index));
            if (x >= left && x < left + width) { select(index); return; }
            left += width + GAP;
        }
    }

    private void select(int index) {
        if (index == selectedIndex || index < 0 || index >= options.size()) return;
        selectedIndex = index;
        setMessage(options.get(index).label());
        revealSelection();
        selected.accept(options.get(index));
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (!active || !visible || !overflowing() || !new GuiBounds(getX(), getY(), getWidth(), getHeight()).contains(x, y)) return false;
        double delta = horizontal != 0 ? horizontal : -vertical;
        scroll = Math.clamp(scroll + delta * 36, 0, maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (!active || !visible || !isFocused() || options.isEmpty()) return false;
        switch (event.key()) {
            case InputConstants.KEY_LEFT -> select(selectedIndex - 1);
            case InputConstants.KEY_RIGHT -> select(selectedIndex + 1);
            case InputConstants.KEY_HOME -> select(0);
            case InputConstants.KEY_END -> select(options.size() - 1);
            default -> { return false; }
        }
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}

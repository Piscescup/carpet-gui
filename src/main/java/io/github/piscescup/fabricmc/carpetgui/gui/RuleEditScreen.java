package io.github.piscescup.fabricmc.carpetgui.gui;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.piscescup.fabricmc.carpetgui.gui.layout.GuiBounds;
import io.github.piscescup.fabricmc.carpetgui.gui.model.*;
import io.github.piscescup.fabricmc.carpetgui.gui.render.GuiTheme;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.DropdownWidget;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.GuiButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.LinkedHashSet;
import java.util.function.Consumer;

/** A local input draft; only Apply submits, with backend permission/validation checked again. */
public final class RuleEditScreen extends AbstractConfigScreen {
    private final EditableRuleView rule;
    private final Consumer<Component> feedback;
    private GuiBounds frame;
    private AbstractWidget input;
    private DropdownWidget<ValueOption> dropdown;
    private GuiButton apply;
    private String draft;
    private Component status = Component.empty();
    private int inputY;

    public RuleEditScreen(Screen parent, EditableRuleView rule, Consumer<Component> feedback) {
        super(parent, rule.label());
        this.rule = rule;
        this.feedback = feedback;
        draft = rule.value();
    }

    @Override protected void init() {
        int w = Math.min(420, width - 32);
        int h = Math.min(230, height - 24);
        frame = new GuiBounds((width - w) / 2, (height - h) / 2, w, h);
        inputY = frame.top() + Math.min(108, frame.height() / 2);
        dropdown = null;
        RuleEditor editor = rule.editor();
        if (editor.inputKind() == RuleEditor.InputKind.BOOLEAN) {
            input = addRenderableWidget(new GuiButton(frame.left() + 14, inputY, w - 28, 20, valueLabel(draft), button -> {
                draft = Boolean.toString(!Boolean.parseBoolean(draft));
                button.setMessage(valueLabel(draft));
            }));
        } else if (editor.strict() && !editor.suggestions().isEmpty()) {
            var choices = new LinkedHashSet<>(editor.suggestions());
            choices.add(draft);
            var options = choices.stream().map(value -> new ValueOption(value, Component.literal(value))).toList();
            dropdown = addRenderableWidget(new DropdownWidget<>(new GuiBounds(frame.left() + 14, inputY, w - 28, 20),
                    Component.translatable("carpet-gui.edit.value"), options, draft,
                    () -> new GuiBounds(10, 28, width - 20, Math.max(26, height - 64)), option -> draft = option.id()));
            input = dropdown;
        } else {
            EditBox box = new EditBox(font, frame.left() + 14, inputY, w - 28, 20, rule.label());
            box.setMaxLength(256);
            box.setValue(draft);
            box.setResponder(value -> draft = value);
            input = addRenderableWidget(box);
        }
        int buttonWidth = (w - 34) / 2;
        apply = addRenderableWidget(new GuiButton(frame.left() + 14, frame.bottom() - 28, buttonWidth, 20,
                Component.translatable("carpet-gui.edit.apply"), ignored -> submit()));
        addRenderableWidget(new GuiButton(frame.left() + 20 + buttonWidth, frame.bottom() - 28, buttonWidth, 20,
                Component.translatable("carpet-gui.edit.cancel"), ignored -> onClose()));
        updateEnabled();
        setInitialFocus(input);
    }

    private static Component valueLabel(String value) {
        return Component.literal(value).withStyle(Boolean.parseBoolean(value) ? ChatFormatting.GREEN : ChatFormatting.RED);
    }
    private void updateEnabled() {
        boolean editable = rule.editor().editable();
        input.active = editable;
        apply.active = editable;
        if (input instanceof EditBox box) box.setEditable(editable);
        if (!editable && dropdown != null) dropdown.closePopup();
    }
    private void submit() {
        RuleEditResult result = rule.editor().submit(draft);
        status = result.message();
        if (result.accepted()) {
            feedback.accept(result.message());
            onClose();
        }
    }
    @Override public void tick() { updateEnabled(); }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(frame.left(), frame.top(), frame.right(), frame.bottom(), 0xEE101010);
        graphics.outline(frame.left(), frame.top(), frame.width(), frame.height(), GuiTheme.LINE);
        graphics.text(font, GuiTheme.fit(font, rule.label().getString(), frame.width() - 28), frame.left() + 14, frame.top() + 12, GuiTheme.TEXT);
        GuiTheme.smallText(graphics, font, rule.id(), frame.left() + 14, frame.top() + 25, frame.width() - 28);
        int y = frame.top() + 40;
        for (var line : font.split(rule.description(), frame.width() - 28)) {
            if (y + 10 >= inputY - 16) break;
            graphics.text(font, line, frame.left() + 14, y, GuiTheme.MUTED);
            y += 10;
        }
        graphics.text(font, Component.translatable("carpet-gui.edit.value"), frame.left() + 14, inputY - 12, GuiTheme.TEXT);
        Component note = !rule.editor().editable() ? rule.editor().disabledReason()
                : status.getString().isEmpty() ? Component.translatable("carpet-gui.edit.session_only") : status;
        graphics.text(font, GuiTheme.fit(font, note.getString(), frame.width() - 28), frame.left() + 14, inputY + 30, GuiTheme.MUTED);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (dropdown != null) dropdown.extractOverlay(graphics, mouseX, mouseY);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (dropdown != null && dropdown.isOpen()) return dropdown.mouseClicked(event, doubleClick);
        return super.mouseClicked(event, doubleClick);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (dropdown != null && dropdown.isOpen()) return dropdown.mouseScrolled(x, y, horizontal, vertical);
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    @Override public void mouseMoved(double x, double y) {
        super.mouseMoved(x, y);
        if (dropdown != null && dropdown.isOpen()) dropdown.mouseMoved(x, y);
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (dropdown != null && dropdown.isOpen() && dropdown.keyPressed(event)) return true;
        if ((event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) && input instanceof EditBox) {
            submit();
            return true;
        }
        return super.keyPressed(event);
    }
    private record ValueOption(String id, Component label) implements DropdownOption {}
}

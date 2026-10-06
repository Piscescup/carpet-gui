package io.github.piscescup.fabricmc.carpetgui.gui.widget;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.piscescup.fabricmc.carpetgui.gui.model.EditableRuleView;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleEditResult;
import io.github.piscescup.fabricmc.carpetgui.gui.layout.GuiBounds;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import java.util.function.Consumer;

/** Inline text draft. Enter/blur submits once; live updates never overwrite an unfinished edit. */
public final class RuleTextBox extends EditBox {
    private final EditableRuleView rule;
    private final Consumer<RuleEditResult> feedback;
    private String observed;
    private String lastAttempt;
    private boolean dirty;
    private boolean synchronizing;

    public RuleTextBox(Font font, GuiBounds bounds, EditableRuleView rule, Consumer<RuleEditResult> feedback) {
        super(font, bounds.left(), bounds.top(), bounds.width(), bounds.height(), rule.label());
        this.rule = rule;
        this.feedback = feedback;
        observed = rule.value();
        setMaxLength(256);
        setValue(observed);
        setResponder(text -> {
            if (synchronizing) return;
            dirty = !text.equals(observed);
            lastAttempt = null;
            setTextColor(0xFFE0E0E0);
        });
    }

    public boolean hasDraft() { return dirty; }

    public void synchronizeValue() {
        if (!dirty) updateFromRule();
    }

    private void updateFromRule() {
        observed = rule.value();
        if (!getValue().equals(observed)) {
            synchronizing = true;
            try { setValue(observed); } finally { synchronizing = false; }
        }
    }

    public void cancelDraft() {
        dirty = false;
        lastAttempt = null;
        setTextColor(0xFFE0E0E0);
        updateFromRule();
    }

    public void commit() {
        if (!dirty || !active || getValue().equals(lastAttempt)) return;
        if (getValue().equals(rule.value())) { cancelDraft(); return; }
        lastAttempt = getValue();
        RuleEditResult result = rule.editor().submit(lastAttempt, feedback);
        feedback.accept(result);
        if (result.accepted()) cancelDraft(); // Show authoritative values, not a fabricated server acknowledgement.
        else setTextColor(0xFFFF5555);
    }

    @Override public void setFocused(boolean focused) {
        boolean leaving = isFocused() && !focused;
        super.setFocused(focused);
        if (leaving && rule != null) commit();
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (active && isFocused() && (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER)) {
            commit();
            return true;
        }
        return super.keyPressed(event);
    }
}

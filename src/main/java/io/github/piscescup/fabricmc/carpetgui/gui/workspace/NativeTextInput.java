package io.github.piscescup.fabricmc.carpetgui.gui.workspace;

import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
import com.thecsdev.commonmc.api.client.gui.util.TInputContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_ESCAPE;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_ENTER;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_RETURN;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_TAB;
import static org.lwjgl.sdl.SDLMouse.SDL_BUTTON_LEFT;

/** Native EditBox bridge: cursor, clipboard and Chinese IME, with TCDCommons focus/events. */
public final class NativeTextInput extends TElement {
    private final EditBox box;
    private final Runnable commit;
    private final Runnable cancel;
    private boolean enabled = true;

    public NativeTextInput(Component hint, String value, Consumer<String> changed, Runnable commit, Runnable cancel) {
        this.commit = commit;
        this.cancel = cancel;
        box = new EditBox(Minecraft.getInstance().font, 0, 0, 100, 20, hint);
        box.setMaxLength(256);
        box.setHint(hint);
        box.setValue(value);
        box.setResponder(changed);
        box.setTextShadow(false);
        focusableProperty().set(true, NativeTextInput.class);
        boundsProperty().addChangeListener((property, oldBounds, bounds) -> {
            // Local native coordinates keep rendering, caret placement and drag selection at the same scale.
            box.setX(0); box.setY(0);
            box.setWidth(Math.max(1, (int) Math.ceil(bounds.width / WorkspaceStyle.TEXT_SCALE)));
            box.setHeight(Math.max(1, (int) Math.ceil(bounds.height / WorkspaceStyle.TEXT_SCALE)));
        });
    }

    public String value() { return box.getValue(); }
    public void setValue(String value) { if (!value.equals(box.getValue())) box.setValue(value); }
    public void setEnabled(boolean value) { enabled = value; box.active = value; box.setEditable(value); }
    @Override public boolean isFocusable() { return enabled && super.isFocusable(); }
    @Override protected void focusGainedCallback() { box.setFocused(true); }
    @Override protected void focusLostCallback() { box.setFocused(false); commit.run(); }
    public boolean charTyped(CharacterEvent event) { return enabled && isFocused() && box.charTyped(event); }
    public boolean preeditUpdated(PreeditEvent event) { return enabled && isFocused() && box.preeditUpdated(event); }

    @Override public void renderCallback(TGuiGraphics graphics) {
        var bounds = getBounds();
        var pose = graphics.getNative().pose();
        pose.pushMatrix();
        try {
            pose.translate(bounds.x, bounds.y).scale((float) WorkspaceStyle.TEXT_SCALE);
            box.extractRenderState(graphics.getNative(), (int) ((graphics.getMouseX() - bounds.x) / WorkspaceStyle.TEXT_SCALE),
                (int) ((graphics.getMouseY() - bounds.y) / WorkspaceStyle.TEXT_SCALE), graphics.getDeltaTicks());
        } finally { pose.popMatrix(); }
    }

    @Override public boolean inputCallback(TInputContext.InputDiscoveryPhase phase, TInputContext context) {
        if (phase != TInputContext.InputDiscoveryPhase.MAIN || !enabled) return false;
        if (context.getInputType() == TInputContext.InputType.MOUSE_PRESS) {
            if (context.getMouseButton() != SDL_BUTTON_LEFT) return false;
            box.mouseClicked(mouseEvent(context), false);
            return true;
        }
        if (!isFocused()) return false;
        if (context.getInputType() == TInputContext.InputType.MOUSE_DRAG) {
            box.mouseDragged(mouseEvent(context), context.getMouseDeltaX() / WorkspaceStyle.TEXT_SCALE,
                context.getMouseDeltaY() / WorkspaceStyle.TEXT_SCALE);
            return true;
        }
        if (context.getInputType() == TInputContext.InputType.MOUSE_RELEASE) {
            box.mouseReleased(mouseEvent(context));
            return true;
        }
        if (context.getInputType() == TInputContext.InputType.KEY_PRESS) {
            int scan = context.getScanCode();
            if (scan == SDL_SCANCODE_TAB) return false;
            if (scan == SDL_SCANCODE_ESCAPE) { cancel.run(); return true; }
            if (scan == SDL_SCANCODE_RETURN || scan == SDL_SCANCODE_KP_ENTER) { commit.run(); return true; }
            // Minecraft 26.3 expects (SDL scancode, SDL keycode, modifiers), unlike TInputContext's factory order.
            box.keyPressed(new KeyEvent(scan, context.getKeyCode(), context.getModifiers()));
            // Native text events deliver Unicode. Consume even printable keys to avoid TCD's ASCII fallback duplicates.
            return true;
        }
        if (context.getInputType() == TInputContext.InputType.CHAR_TYPE) {
            return box.charTyped(new CharacterEvent(context.getCharacter()));
        }
        return false;
    }

    private MouseButtonEvent mouseEvent(TInputContext context) {
        var bounds = getBounds();
        return new MouseButtonEvent((context.getMouseX() - bounds.x) / WorkspaceStyle.TEXT_SCALE,
            (context.getMouseY() - bounds.y) / WorkspaceStyle.TEXT_SCALE,
            new MouseButtonInfo(context.getMouseButton(), context.getModifiers() == null ? 0 : context.getModifiers()));
    }
}

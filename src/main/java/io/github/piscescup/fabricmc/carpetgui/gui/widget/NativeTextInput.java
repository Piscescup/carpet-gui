/*
 * This file is part of the Carpet GUI project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  Fallen_Breath and contributors
 *
 * Carpet GUI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Carpet GUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Carpet GUI.  If not, see <https://www.gnu.org/licenses/>.
 */

//#if MC >= 12111
package io.github.piscescup.fabricmc.carpetgui.gui.widget;

import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
import com.thecsdev.commonmc.api.client.gui.util.TInputContext;
import io.github.piscescup.fabricmc.carpetgui.gui.workspace.WorkspaceStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
//#if MC >= 260000
import net.minecraft.client.input.PreeditEvent;
//#endif
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

//#if MC >= 260300
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_ESCAPE;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_ENTER;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_RETURN;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_TAB;
import static org.lwjgl.sdl.SDLMouse.SDL_BUTTON_LEFT;
//#else
//$$ import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
//$$ import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
//$$ import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER;
//$$ import static org.lwjgl.glfw.GLFW.GLFW_KEY_TAB;
//$$ import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
//#endif

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
    //#if MC >= 260000
    public boolean preeditUpdated(PreeditEvent event) { return enabled && isFocused() && box.preeditUpdated(event); }
    //#endif

    @Override public void renderCallback(TGuiGraphics graphics) {
        var bounds = getBounds();
        var pose = graphics.getNative().pose();
        pose.pushMatrix();
        try {
            pose.translate(bounds.x, bounds.y).scale((float) WorkspaceStyle.TEXT_SCALE);
            //#if MC >= 260000
            box.extractRenderState(graphics.getNative(), (int) ((graphics.getMouseX() - bounds.x) / WorkspaceStyle.TEXT_SCALE),
                (int) ((graphics.getMouseY() - bounds.y) / WorkspaceStyle.TEXT_SCALE), graphics.getDeltaTicks());
            //#else
            //$$ box.renderWidget(graphics.getNative(), (int) ((graphics.getMouseX() - bounds.x) / WorkspaceStyle.TEXT_SCALE),
            //$$     (int) ((graphics.getMouseY() - bounds.y) / WorkspaceStyle.TEXT_SCALE), graphics.getDeltaTicks());
            //#endif
        } finally { pose.popMatrix(); }
    }

    @Override public boolean inputCallback(TInputContext.InputDiscoveryPhase phase, TInputContext context) {
        if (phase != TInputContext.InputDiscoveryPhase.MAIN || !enabled) return false;
        if (context.getInputType() == TInputContext.InputType.MOUSE_PRESS) {
            //#if MC >= 260300
            if (context.getMouseButton() != SDL_BUTTON_LEFT) return false;
            //#else
            //$$ if (context.getMouseButton() != GLFW_MOUSE_BUTTON_LEFT) return false;
            //#endif
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
            //#if MC >= 260300
            int scan = context.getScanCode();
            if (scan == SDL_SCANCODE_TAB) return false;
            if (scan == SDL_SCANCODE_ESCAPE) { cancel.run(); return true; }
            if (scan == SDL_SCANCODE_RETURN || scan == SDL_SCANCODE_KP_ENTER) { commit.run(); return true; }
            // Minecraft 26.3 expects (SDL scancode, SDL keycode, modifiers), unlike TInputContext's factory order.
            box.keyPressed(new KeyEvent(scan, context.getKeyCode(), context.getModifiers()));
            //#else
            //$$ int key = context.getKeyCode();
            //$$ if (key == GLFW_KEY_TAB) return false;
            //$$ if (key == GLFW_KEY_ESCAPE) { cancel.run(); return true; }
            //$$ if (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER) { commit.run(); return true; }
            //$$ box.keyPressed(new KeyEvent(key, context.getScanCode(), context.getKeyModifiers()));
            //#endif
            // Native text events deliver Unicode. Consume even printable keys to avoid TCD's ASCII fallback duplicates.
            return true;
        }
        if (context.getInputType() == TInputContext.InputType.CHAR_TYPE) {
            //#if MC >= 260000
            return box.charTyped(new CharacterEvent(context.getCharacter()));
            //#else
            //$$ return box.charTyped(new CharacterEvent(context.getCharacter(), context.getKeyModifiers()));
            //#endif
        }
        return false;
    }

    private MouseButtonEvent mouseEvent(TInputContext context) {
        var bounds = getBounds();
        return new MouseButtonEvent((context.getMouseX() - bounds.x) / WorkspaceStyle.TEXT_SCALE,
            (context.getMouseY() - bounds.y) / WorkspaceStyle.TEXT_SCALE,
            //#if MC >= 260300
            new MouseButtonInfo(context.getMouseButton(), context.getModifiers() == null ? 0 : context.getModifiers()));
            //#else
            //$$ new MouseButtonInfo(context.getMouseButton(), 0));
            //#endif
    }
}
//#endif

package io.github.piscescup.fabricmc.carpetgui.gui.workspace;

import com.thecsdev.common.math.Bounds2i;
import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
import io.github.piscescup.fabricmc.carpetgui.References;
import net.minecraft.resources.Identifier;

/** Resource textures are loaded and cached by Minecraft, never decoded per frame. */
final class WorkspaceActionTextures {
    enum Icon {
        LOCK_OPEN("lock_open"), LOCK_CLOSED("lock_closed"), RESET("reset"),
        FAVORITE_OFF("favorite_off"), FAVORITE_ON("favorite_on");

        private final Identifier texture;
        Icon(String name) {
            texture = References.fromPath("textures/gui/icons/actions/" + name + ".png");
        }
    }

    private WorkspaceActionTextures() {}

    static void draw(TGuiGraphics graphics, Bounds2i bounds, Icon icon, int tint) {
        int size = Math.max(1, Math.min(14, Math.min(bounds.width, bounds.height)));
        graphics.drawTexture(icon.texture, bounds.x + (bounds.width - size) / 2,
            bounds.y + (bounds.height - size) / 2, size, size, tint);
    }
}

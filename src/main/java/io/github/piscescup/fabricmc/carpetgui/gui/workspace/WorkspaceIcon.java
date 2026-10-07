package io.github.piscescup.fabricmc.carpetgui.gui.workspace;

import com.thecsdev.commonmc.api.client.gui.misc.TTextureElement;
import io.github.piscescup.fabricmc.carpetgui.References;
import net.minecraft.resources.Identifier;

/** Resource-backed original PNG icons; replace the files without changing layout code. */
public final class WorkspaceIcon extends TTextureElement {
    public enum Kind {
        SORT("filter_sort.png"),
        GROUP("filter_group.png"),
        DISTANCE("filter_unit_dist.png"),
        TIME("filter_unit_time.png");

        private final Identifier texture;
        Kind(String filename) {
            texture = References.fromPath("textures/gui/icons/" + filename);
        }
        public Identifier texture() { return texture; }
    }

    public WorkspaceIcon(Kind kind) {
        super(kind.texture());
        modeProperty().set(Mode.TEXTURE, WorkspaceIcon.class);
        hoverableProperty().set(false, WorkspaceIcon.class);
        focusableProperty().set(false, WorkspaceIcon.class);
    }

}

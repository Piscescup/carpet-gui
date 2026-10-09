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

//#if MC >= 260000
package io.github.piscescup.fabricmc.carpetgui.mixin;

import com.thecsdev.commonmc.api.client.gui.screen.TScreenWrapper;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.piscescup.fabricmc.carpetgui.gui.workspace.CarpetWorkspaceScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Gives this workspace a fixed background blur without changing the user's option. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    private static final int CARPET_GUI_BLUR_RADIUS = 8;

    @ModifyExpressionValue(
        method = "extractOptions",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getMenuBackgroundBlurriness()I")
    )
    private int carpetGui$increaseWorkspaceBlur(int configured) {
        //#if MC >= 260200
        var current = Minecraft.getInstance().gui.screen();
        //#else
        //$$ var current = Minecraft.getInstance().screen;
        //#endif
        if (current instanceof TScreenWrapper<?> wrapper
            && wrapper.getTargetTScreen() instanceof CarpetWorkspaceScreen) {
            return CARPET_GUI_BLUR_RADIUS;
        }
        return configured;
    }
}
//#endif

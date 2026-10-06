package io.github.piscescup.fabricmc.carpetgui.mixin;

import io.github.piscescup.fabricmc.carpetgui.integration.vanilla.VanillaRuleStore;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundGameRuleValuesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleGameRuleValues", at = @At("TAIL"))
    private void carpetGui$receiveGameRules(ClientboundGameRuleValuesPacket packet, CallbackInfo ci) {
        VanillaRuleStore.accept(packet.values());
    }
}

package com.enochmods.enoch.mixin;

import com.enochmods.enoch.Enoch;
import com.enochmods.enoch.SigilData;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class SwapHandsMixin {
    @Shadow public ServerPlayer player;

    // This point follows PacketUtils' thread handoff and vanilla's client-loaded guard.
    @Inject(method = "handlePlayerAction", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V", shift = At.Shift.AFTER),
            cancellable = true)
    private void enoch$useSigil(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (packet.getAction() == ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND
                && SigilData.isSigil(player.getMainHandItem())) {
            ci.cancel();
            Enoch.activate(player);
        }
    }
}

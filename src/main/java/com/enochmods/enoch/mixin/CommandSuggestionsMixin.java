package com.enochmods.enoch.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla refreshes abilities on mode changes, but not the permission-filtered command tree. */
@Mixin(ServerPlayerGameMode.class)
public abstract class CommandSuggestionsMixin {
    @Shadow @Final protected ServerPlayer player;

    @Inject(method = "changeGameModeForPlayer", at = @At("RETURN"))
    private void enoch$refreshCommands(GameType mode, CallbackInfoReturnable<Boolean> result) {
        if (result.getReturnValue()) player.level().getServer().getCommands().sendCommands(player);
    }
}

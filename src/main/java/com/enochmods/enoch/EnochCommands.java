package com.enochmods.enoch;

import com.enochmods.enoch.state.WorldConfig;
import com.enochmods.enoch.state.WorldConfigStore;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.io.IOException;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class EnochCommands {
    private EnochCommands() { }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                Function<MinecraftServer, WorldConfigStore> configurations) {
        dispatcher.register(Commands.literal("enoch")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("give")
                        .executes(context -> give(context.getSource(), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> give(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("config")
                        .then(Commands.literal("show").executes(context -> show(context.getSource(), configurations)))
                        .then(Commands.literal("cooldown")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(0, WorldConfig.MAX_COOLDOWN_SECONDS))
                                        .executes(context -> set(context.getSource(), configurations,
                                                new WorldConfig(IntegerArgumentType.getInteger(context, "seconds"))))))
                        .then(Commands.literal("reset")
                                .executes(context -> set(context.getSource(), configurations, WorldConfig.DEFAULT)))));
    }

    private static int give(CommandSourceStack source, ServerPlayer player) {
        player.getInventory().placeItemBackInInventory(SigilData.createSigil());
        source.sendSuccess(() -> Component.literal("Enoch: gave a sigil to " + player.getName().getString()), false);
        return 1;
    }

    private static int show(CommandSourceStack source, Function<MinecraftServer, WorldConfigStore> configurations) {
        WorldConfigStore store = configurations.apply(source.getServer());
        if (store == null) return unavailable(source);
        source.sendSuccess(() -> Component.literal("Enoch: cooldown = " + store.get().cooldownSeconds()
                + " seconds (default 10, range 0..3600). Changes apply to future returns."), false);
        return 1;
    }

    private static int set(CommandSourceStack source, Function<MinecraftServer, WorldConfigStore> configurations,
                           WorldConfig next) {
        WorldConfigStore store = configurations.apply(source.getServer());
        if (store == null) return unavailable(source);
        try {
            store.set(next);
        } catch (IOException exception) {
            Enoch.LOGGER.error("Enoch configuration save failed; setting unchanged", exception);
            source.sendFailure(Component.literal("Enoch: could not save configuration; setting unchanged. Check the server log."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Enoch: cooldown saved as " + next.cooldownSeconds()
                + " seconds. Existing cooldown deadlines are unchanged."), false);
        return 1;
    }

    private static int unavailable(CommandSourceStack source) {
        source.sendFailure(Component.literal("Enoch: world configuration unavailable. Check the server log."));
        return 0;
    }
}

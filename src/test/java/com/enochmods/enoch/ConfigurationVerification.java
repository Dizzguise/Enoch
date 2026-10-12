package com.enochmods.enoch;

import com.enochmods.enoch.state.Anchor;
import com.enochmods.enoch.state.AnchorStore;
import com.enochmods.enoch.state.WorldConfig;
import com.enochmods.enoch.state.WorldConfigStore;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/** Exercises actual permission-gated command trees, vanilla item components and world config persistence. */
public final class ConfigurationVerification {
    private static int checks;

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createLookup()).forEach(pending -> pending.apply());
        Path directory = Files.createTempDirectory("enoch-config-test-");
        try {
            Path file = directory.resolve("world-a/enoch-config.json");
            WorldConfigStore config = new WorldConfigStore(file);
            config.load();
            check(config.get().equals(WorldConfig.DEFAULT), "missing config defaults to ten seconds");
            config.set(new WorldConfig(30));
            WorldConfigStore restarted = new WorldConfigStore(file);
            restarted.load();
            check(restarted.get().cooldownSeconds() == 30, "restart preserves configured cooldown");
            check(!Files.exists(file.resolveSibling("enoch-config.json.tmp")), "atomic config replacement consumes temp file");
            WorldConfigStore otherWorld = new WorldConfigStore(directory.resolve("world-b/enoch-config.json"));
            otherWorld.load();
            check(otherWorld.get().equals(WorldConfig.DEFAULT), "different worlds have independent config");
            expect(IllegalArgumentException.class, () -> new WorldConfig(-1), "negative cooldown rejected");
            expect(IllegalArgumentException.class, () -> new WorldConfig(3601), "excessive cooldown rejected");
            check(new WorldConfig(3600).cooldownMillis() == 3_600_000, "maximum cooldown converts correctly");

            UUID player = UUID.randomUUID();
            Anchor anchor = new Anchor("minecraft:overworld", 0, 64, 0, 0, 0);
            AnchorStore ledger = new AnchorStore(directory.resolve("world-a/enoch-anchors.json"));
            ledger.mark(player, anchor, 1000);
            ledger.returned(player, 1000, restarted.get());
            check(ledger.get(player).anchor() == null, "configured return consumes anchor once");
            check(ledger.get(player).remainingMillis(1000) == 30_000, "return uses configured cooldown");
            restarted.set(new WorldConfig(0));
            check(ledger.get(player).cooldownUntilMillis() == 31_000, "config change preserves active deadline");
            ledger.mark(player, anchor, 31_000);
            ledger.returned(player, 31_000, restarted.get());
            check(ledger.get(player).remainingMillis(31_000) == 0, "zero cooldown is immediately ready");
            ledger.mark(player, anchor, 31_000);
            check(ledger.get(player).anchor().equals(anchor), "zero cooldown permits next anchor");

            CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
            EnochCommands.register(dispatcher, server -> restarted);
            CommandSourceStack admin = source(PermissionSet.ALL_PERMISSIONS);
            CommandSourceStack regular = source(PermissionSet.NO_PERMISSIONS);
            CommandSourceStack moderator = source(permission -> permission.equals(Permissions.COMMANDS_MODERATOR));
            check(!dispatcher.getRoot().getChild("enoch").canUse(regular), "regular players cannot see admin commands");
            check(!dispatcher.getRoot().getChild("enoch").canUse(moderator), "moderator below gamemaster cannot configure");
            for (String command : new String[] {"enoch config show", "enoch config cooldown 45", "enoch config reset", "enoch give"}) {
                expect(CommandSyntaxException.class, () -> dispatcher.execute(command, regular), "regular player cannot run " + command);
            }
            check(dispatcher.execute("enoch config show", admin) == 1, "administrator can inspect configuration");
            check(dispatcher.execute("enoch config cooldown 45", admin) == 1, "administrator can set cooldown");
            WorldConfigStore commandRestart = new WorldConfigStore(file);
            commandRestart.load();
            check(commandRestart.get().cooldownSeconds() == 45, "command saves configuration durably");
            expect(CommandSyntaxException.class, () -> dispatcher.execute("enoch config cooldown -1", admin), "command rejects negative seconds");
            expect(CommandSyntaxException.class, () -> dispatcher.execute("enoch config cooldown 3601", admin), "command rejects excessive seconds");
            check(dispatcher.execute("enoch config reset", admin) == 1 && restarted.get().equals(WorldConfig.DEFAULT), "reset restores default");
            check(ledger.get(player).anchor().equals(anchor), "config commands preserve existing anchors");

            for (String invalid : new String[] {"{broken", "{\"version\":1,\"cooldownSeconds\":-1}",
                    "{\"version\":1,\"cooldownSeconds\":1.5}", "{\"version\":1,\"cooldownSeconds\":\"30\"}",
                    "{\"version\":2,\"cooldownSeconds\":30}"}) {
                Files.writeString(file, invalid);
                expect(IOException.class, () -> new WorldConfigStore(file).load(), "invalid config fails closed");
                check(Files.readString(file).equals(invalid), "invalid config original preserved");
            }
            Path blocked = directory.resolve("blocked");
            Files.writeString(blocked, "blocker");
            WorldConfigStore failed = new WorldConfigStore(blocked.resolve("enoch-config.json"));
            expect(IOException.class, () -> failed.set(new WorldConfig(60)), "unwritable configuration save fails");
            check(failed.get().equals(WorldConfig.DEFAULT), "failed save does not change live config");
            Files.delete(blocked);
            failed.set(new WorldConfig(60));
            check(failed.get().cooldownSeconds() == 60, "config save retry succeeds");

            ItemStack sigil = SigilData.createSigil();
            check(sigil.is(Items.AMETHYST_SHARD) && SigilData.isSigil(sigil), "sigil uses vanilla amethyst identity and tag");
            check(sigil.getMaxStackSize() == 1, "sigil is unstackable");
            check(sigil.getHoverName().getString().equals("Enoch Sigil"), "sigil name uses client-readable literal text");
            check(sigil.get(DataComponents.LORE).lines().size() == 2, "sigil instructions use vanilla lore");
            check(!SigilData.isSigil(new ItemStack(Items.AMETHYST_SHARD)), "ordinary amethyst is not a sigil");
            ItemStack renamed = new ItemStack(Items.AMETHYST_SHARD);
            renamed.set(DataComponents.CUSTOM_NAME, sigil.get(DataComponents.CUSTOM_NAME));
            check(!SigilData.isSigil(renamed), "anvil-style rename does not mint a sigil");
            ItemStack wrongItem = new ItemStack(Items.DIAMOND);
            wrongItem.set(DataComponents.CUSTOM_DATA, sigil.get(DataComponents.CUSTOM_DATA));
            check(!SigilData.isSigil(wrongItem), "flag on another item is rejected");
            check(SigilData.isSigil(sigil.copy()), "copied item preserves sigil identity");
            check(!SigilData.isSigil(ItemStack.EMPTY), "empty hand is not a sigil");
            System.out.println("Enoch config/command/item verification: " + checks + " checks passed.");
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    private static CommandSourceStack source(PermissionSet permissions) {
        return new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, permissions,
                "test", Component.literal("test"), null, null);
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void expect(Class<? extends Throwable> type, ThrowingRunnable action, String message) throws Exception {
        checks++;
        try { action.run(); }
        catch (Throwable thrown) {
            if (type.isInstance(thrown)) return;
            throw new AssertionError(message, thrown);
        }
        throw new AssertionError(message);
    }
    @FunctionalInterface private interface ThrowingRunnable { void run() throws Exception; }
}

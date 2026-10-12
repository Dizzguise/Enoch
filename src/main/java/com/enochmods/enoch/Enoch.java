package com.enochmods.enoch;

import com.enochmods.enoch.state.Anchor;
import com.enochmods.enoch.state.AnchorStore;
import com.enochmods.enoch.state.PlayerState;
import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Enoch implements ModInitializer {
    public static final String MOD_ID = "enoch";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Identifier SIGIL_ID = Identifier.fromNamespaceAndPath(MOD_ID, "sigil");
    public static final Item SIGIL = new SigilItem(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, SIGIL_ID)).stacksTo(1).rarity(Rarity.RARE));
    // Keyed by server identity, never by global dimension identifiers or game-directory path.
    private static final Map<MinecraftServer, AnchorStore> STORES = new IdentityHashMap<>();

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.ITEM, SIGIL_ID, SIGIL);
        CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                Identifier.withDefaultNamespace("tools_and_utilities"))).register(entries -> entries.accept(SIGIL));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            AnchorStore store = new AnchorStore(server.getWorldPath(LevelResource.DATA).resolve("enoch-anchors.json"));
            try {
                store.load();
                STORES.put(server, store);
                LOGGER.info("Enoch anchor ledger loaded for {}", server.getWorldPath(LevelResource.ROOT));
            } catch (IOException exception) {
                LOGGER.error("Enoch disabled for this world because its ledger could not be loaded", exception);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> save(STORES.get(server)));
        ServerLifecycleEvents.SERVER_STOPPED.register(STORES::remove);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                AnchorStore store = STORES.get(player.level().getServer());
                if (store != null) { store.died(player.getUUID()); save(store); }
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            AnchorStore store = STORES.get(server);
            if (server.getTickCount() % 100 == 0) save(store);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.getMainHandItem().is(SIGIL) && player.isAlive()) {
                    player.sendOverlayMessage(status(store, player));
                }
            }
        });
    }

    public static void activate(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) return;
        AnchorStore store = STORES.get(player.level().getServer());
        if (store == null) { fail(player, "Enoch: world anchor data unavailable"); return; }
        PlayerState state = store.get(player.getUUID());
        long now = System.currentTimeMillis();
        if (state.remainingMillis(now) > 0) {
            fail(player, "Enoch: recovering " + seconds(state.remainingMillis(now)) + "s");
            return;
        }
        if (player.isPassenger() || player.isSleeping() || player.isChangingDimension()) {
            fail(player, "Enoch: stand freely before using the sigil");
            return;
        }
        if (state.anchor() == null) {
            Anchor anchor = new Anchor(player.level().dimension().identifier().toString(),
                    player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            store.mark(player.getUUID(), anchor, now);
            save(store);
            player.sendOverlayMessage(Component.literal("Enoch: anchor set · " + describe(anchor)));
            sound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f);
            return;
        }
        Anchor anchor = state.anchor();
        ServerLevel destination = player.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION,
                Identifier.parse(anchor.dimension())));
        if (destination == null) { fail(player, "Enoch: anchor dimension unavailable; anchor retained"); return; }
        // Validate standing height even if the player currently crouches, swims, or glides.
        AABB bounds = player.getDimensions(Pose.STANDING).makeBoundingBox(new Vec3(anchor.x(), anchor.y(), anchor.z()));
        if (bounds.minY < destination.getMinY() || bounds.maxY > destination.getMaxY() + 1.0
                || !destination.getWorldBorder().isWithinBounds(bounds)) {
            fail(player, "Enoch: anchor outside world limits; anchor retained");
            return;
        }
        try {
            // Every chunk touched by the player box must exist before collision testing or teleporting.
            int minX = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ).getX() >> 4;
            int maxX = BlockPos.containing(bounds.maxX, bounds.minY, bounds.maxZ).getX() >> 4;
            int minZ = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ).getZ() >> 4;
            int maxZ = BlockPos.containing(bounds.maxX, bounds.minY, bounds.maxZ).getZ() >> 4;
            for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
                if (destination.getChunk(x, z) == null) {
                    fail(player, "Enoch: anchor chunks unavailable; anchor retained"); return;
                }
            }
            if (!destination.noCollision(player, bounds)) {
                fail(player, "Enoch: anchor obstructed; anchor retained"); return;
            }
            if (!player.teleportTo(destination, anchor.x(), anchor.y(), anchor.z(), Set.of(),
                    anchor.yaw(), anchor.pitch(), true)) {
                fail(player, "Enoch: return failed; anchor retained"); return;
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Enoch return failed for {}", player.getUUID(), exception);
            fail(player, "Enoch: return failed; anchor retained");
            return;
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.setKnownMovement(Vec3.ZERO);
        player.hurtMarked = true;
        player.resetFallDistance();
        store.returned(player.getUUID(), System.currentTimeMillis());
        save(store);
        player.sendOverlayMessage(Component.literal("Enoch: returned · recovering 10s"));
        sound(player, SoundEvents.ENDERMAN_TELEPORT, 1.0f);
    }

    private static Component status(AnchorStore store, ServerPlayer player) {
        if (store == null) return Component.literal("Enoch: world anchor data unavailable");
        PlayerState state = store.get(player.getUUID());
        long remaining = state.remainingMillis(System.currentTimeMillis());
        if (remaining > 0) return Component.literal("Enoch: recovering " + seconds(remaining) + "s");
        return Component.literal(state.anchor() == null ? "Enoch: ready · swap hands to set anchor"
                : "Enoch: " + describe(state.anchor()) + " · swap hands to return");
    }

    private static String describe(Anchor anchor) {
        return String.format(Locale.ROOT, "%s · %.1f, %.1f, %.1f", anchor.dimension(), anchor.x(), anchor.y(), anchor.z());
    }

    private static long seconds(long millis) { return (millis + 999) / 1000; }
    private static void fail(ServerPlayer player, String message) {
        player.sendOverlayMessage(Component.literal(message));
        sound(player, SoundEvents.NOTE_BLOCK_BASS.value(), 0.6f);
    }
    private static void sound(ServerPlayer player, SoundEvent sound, float pitch) {
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.6f, pitch);
    }
    private static void save(AnchorStore store) {
        if (store == null) return;
        try { store.save(); }
        catch (IOException exception) { LOGGER.error("Enoch ledger save failed; will retry while dirty", exception); }
    }
}

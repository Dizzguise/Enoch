package com.enochmods.enoch.state;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-thread-only world ledger. No static player/server caches survive shutdown. */
public final class AnchorStore {
    public static final long COOLDOWN_MILLIS = 10_000;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private final Map<UUID, PlayerState> players = new HashMap<>();
    private boolean dirty;

    public AnchorStore(Path file) { this.file = file; }

    public void load() throws IOException {
        players.clear();
        if (!Files.exists(file)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if (root.get("version").getAsInt() != 1) throw new IllegalArgumentException("Unknown ledger version");
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("players").entrySet()) {
                UUID id = UUID.fromString(entry.getKey());
                JsonObject data = entry.getValue().getAsJsonObject();
                long cooldown = data.get("cooldownUntilMillis").getAsLong();
                Anchor anchor = null;
                if (data.has("anchor") && !data.get("anchor").isJsonNull()) {
                    JsonObject mark = data.getAsJsonObject("anchor");
                    // Construct explicitly so corrupt positions cannot bypass validation.
                    anchor = new Anchor(mark.get("dimension").getAsString(), mark.get("x").getAsDouble(),
                            mark.get("y").getAsDouble(), mark.get("z").getAsDouble(),
                            mark.get("yaw").getAsFloat(), mark.get("pitch").getAsFloat());
                }
                players.put(id, new PlayerState(anchor, cooldown));
            }
            dirty = false;
        } catch (RuntimeException exception) {
            players.clear();
            throw new IOException("Invalid Enoch ledger; preserving original file", exception);
        }
    }

    public PlayerState get(UUID player) { return players.getOrDefault(player, PlayerState.EMPTY); }

    public void mark(UUID player, Anchor anchor, long now) {
        PlayerState old = get(player);
        if (old.remainingMillis(now) > 0 || old.anchor() != null) throw new IllegalStateException("Cannot mark now");
        put(player, new PlayerState(anchor, old.cooldownUntilMillis()));
    }

    /** Called only after the teleport API confirms success. A failed return never mutates this ledger. */
    public void returned(UUID player, long now) {
        if (get(player).anchor() == null) throw new IllegalStateException("No anchor to consume");
        put(player, new PlayerState(null, Math.addExact(now, COOLDOWN_MILLIS)));
    }

    public void died(UUID player) {
        PlayerState old = get(player);
        if (old.anchor() != null) put(player, new PlayerState(null, old.cooldownUntilMillis()));
    }

    private void put(UUID player, PlayerState state) { players.put(player, state); dirty = true; }

    public boolean isDirty() { return dirty; }

    public void save() throws IOException {
        if (!dirty) return;
        Files.createDirectories(file.getParent());
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        JsonObject entries = new JsonObject();
        players.forEach((id, state) -> entries.add(id.toString(), GSON.toJsonTree(state)));
        root.add("players", entries);
        byte[] bytes = GSON.toJson(root).getBytes(StandardCharsets.UTF_8);
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
        dirty = false;
    }
}

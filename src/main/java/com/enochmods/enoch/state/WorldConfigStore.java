package com.enochmods.enoch.state;

import com.google.gson.GsonBuilder;
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

/** Separate world configuration. Changes take effect only after a successful durable save. */
public final class WorldConfigStore {
    private final Path file;
    private WorldConfig config = WorldConfig.DEFAULT;

    public WorldConfigStore(Path file) { this.file = file; }
    public WorldConfig get() { return config; }

    public void load() throws IOException {
        if (!Files.exists(file)) { config = WorldConfig.DEFAULT; return; }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if (integer(root, "version") != 1) throw new IllegalArgumentException("Unknown config version");
            config = new WorldConfig(integer(root, "cooldownSeconds"));
        } catch (RuntimeException exception) {
            throw new IOException("Invalid Enoch configuration; preserving original file", exception);
        }
    }

    private static int integer(JsonObject root, String key) {
        var value = root.getAsJsonPrimitive(key);
        if (!value.isNumber()) throw new IllegalArgumentException("Expected integer: " + key);
        return Integer.parseInt(value.getAsString());
    }

    public void set(WorldConfig next) throws IOException {
        Files.createDirectories(file.getParent());
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.addProperty("cooldownSeconds", next.cooldownSeconds());
        byte[] bytes = new GsonBuilder().setPrettyPrinting().create().toJson(root).getBytes(StandardCharsets.UTF_8);
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
        config = next;
    }
}

package com.enochmods.enoch.state;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Dependency-free regression checks for restart, per-player/world isolation, death, and disk failures. */
public final class LedgerVerification {
    private static int checks;

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("enoch-ledger-test-");
        try {
            UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
            Anchor mark = new Anchor("minecraft:the_nether", 15.999, 70, -16.01, 123.4f, -30);
            Path file = directory.resolve("world-a/enoch-anchors.json");
            AnchorStore store = new AnchorStore(file);
            store.load();
            check(store.get(alice).equals(PlayerState.EMPTY), "new player starts empty");
            store.mark(alice, mark, 1_000);
            check(store.isDirty(), "mark dirties ledger");
            check(store.get(bob).equals(PlayerState.EMPTY), "player state is isolated");
            expect(IllegalStateException.class, () -> store.mark(alice, mark, 1_000), "existing mark cannot be overwritten");
            expect(IllegalStateException.class, () -> store.returned(bob, 1_000), "missing mark cannot be consumed");
            store.save();
            check(!store.isDirty(), "successful save clears dirty state");
            check(!Files.exists(file.resolveSibling("enoch-anchors.json.tmp")), "atomic save consumes temporary file");
            AnchorStore restarted = new AnchorStore(file);
            restarted.load();
            check(restarted.get(alice).anchor().equals(mark), "restart preserves exact dimension/position/view");
            restarted.returned(alice, 2_000);
            check(restarted.get(alice).anchor() == null, "successful return consumes mark");
            check(restarted.get(alice).remainingMillis(2_000) == 10_000, "return starts 10-second cooldown");
            check(restarted.get(alice).remainingMillis(11_999) == 1, "cooldown stays active until deadline");
            check(restarted.get(alice).remainingMillis(12_000) == 0, "cooldown expires at deadline");
            expect(IllegalStateException.class, () -> restarted.mark(alice, mark, 11_999), "cooldown prevents remark");
            restarted.save();
            AnchorStore cooldownRestart = new AnchorStore(file);
            cooldownRestart.load();
            check(cooldownRestart.get(alice).remainingMillis(3_000) == 9_000, "restart preserves wall-clock deadline");
            cooldownRestart.died(alice);
            check(cooldownRestart.get(alice).cooldownUntilMillis() == 12_000, "death retains cooldown");
            cooldownRestart.mark(alice, mark, 12_000);
            cooldownRestart.died(alice);
            check(cooldownRestart.get(alice).anchor() == null, "death clears anchor");
            check(cooldownRestart.get(alice).cooldownUntilMillis() == 12_000, "death with anchor retains deadline");
            AnchorStore otherWorld = new AnchorStore(directory.resolve("world-b/enoch-anchors.json"));
            otherWorld.load();
            check(otherWorld.get(alice).equals(PlayerState.EMPTY), "world state is isolated");
            expect(IllegalArgumentException.class, () -> new Anchor("bad", 0, 64, 0, 0, 0), "invalid dimension rejected");
            expect(IllegalArgumentException.class, () -> new Anchor("minecraft:overworld", Double.NaN, 64, 0, 0, 0), "NaN rejected");
            expect(IllegalArgumentException.class, () -> new Anchor("minecraft:overworld", 0, 64, 0, 0, 91), "invalid pitch rejected");
            Files.writeString(file, "{broken");
            expect(IOException.class, restarted::load, "malformed ledger fails closed");
            check(Files.readString(file).equals("{broken"), "malformed ledger preserved");
            Path invalid = directory.resolve("invalid.json");
            Files.writeString(invalid, "{\"version\":1,\"players\":{\"" + alice + "\":{\"cooldownUntilMillis\":-1}}}");
            expect(IOException.class, () -> new AnchorStore(invalid).load(), "invalid persisted cooldown rejected");
            // Simulate an unwritable destination: its parent is a file, not a directory.
            Path blocked = directory.resolve("blocked");
            Files.writeString(blocked, "blocker");
            AnchorStore retry = new AnchorStore(blocked.resolve("ledger.json"));
            retry.mark(alice, mark, 1_000);
            expect(IOException.class, retry::save, "save failure reported");
            check(retry.isDirty(), "save failure retains dirty state for retry");
            Files.delete(blocked);
            retry.save();
            check(!retry.isDirty(), "save retry succeeds without losing mark");
            System.out.println("Enoch ledger verification: " + checks + " checks passed.");
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
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

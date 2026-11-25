package com.goosker.compass.client.hud;

import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

/** Disk-backed regressions; no Minecraft or Xaero installation required. */
public final class DeathPersistenceTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("compass-death-test");
        DeathWaypointCache.openWorld(dir, "world-a", null);
        DeathWaypointCache.rememberDeath("minecraft:overworld", 101, 64, -300, true);
        CompassMarker first = DeathWaypointCache.merge(List.of()).get(0);
        // Empty Xaero lists on leaving/returning to a dimension cannot evict it.
        for (int i = 0; i < 100; i++) {
            check(DeathWaypointCache.merge(List.of()).contains(first), "lost death during dimension travel");
        }
        DeathWaypointCache.rememberDeath("minecraft:the_nether", 101, 64, -300, true);
        check(DeathWaypointCache.merge(List.of()).size() == 2, "dimensions conflated");
        DeathWaypointCache.rememberDeath("minecraft:the_nether", 101, 64, -300, false);
        check(DeathWaypointCache.merge(List.of()).size() == 2, "vanilla poll duplicated death");
        CompassMarker latest = DeathWaypointCache.merge(List.of()).get(1);
        CompassMarker xaeroCopy = new CompassMarker(MarkerType.DEATH, "Latest Death", 101, 64, -300,
                "minecraft:the_nether", latest.createdAt() + 10, null);
        check(DeathWaypointCache.merge(List.of(xaeroCopy)).size() == 2, "Xaero duplicated vanilla death");
        DeathWaypointCache.disconnect();
        DeathWaypointCache.openWorld(dir, "world-a", null);
        check(DeathWaypointCache.merge(List.of()).contains(first), "lost persisted death after reconnect");
        check(DeathWaypointCache.merge(List.of()).contains(latest), "lost latest persisted death");
        DeathWaypointCache.openWorld(dir, "world-b", null);
        check(DeathWaypointCache.merge(List.of()).isEmpty(), "cross-world contamination");
        DeathWaypointCache.openWorld(dir, "world-a", null);
        check(DeathWaypointCache.merge(List.of()).size() == 2, "opening other world erased cache");
        DeathWaypointCache.rememberDeath("minecraft:the_nether", 101, 64, -300, true);
        check(DeathWaypointCache.merge(List.of()).size() == 3, "repeat death at same point lost");
        // Migrate a V1 file whose grave heuristic falsely marked it recovered.
        DeathWaypointCache.disconnect();
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest("legacy".getBytes(StandardCharsets.UTF_8)));
        Path old = dir.resolve("gooskers-compass/death-cache/" + hash + ".bin");
        try (var out = new DataOutputStream(Files.newOutputStream(old))) {
            out.writeUTF("GSC_DEATH_CACHE_V1"); out.writeInt(1);
            out.writeUTF("Death"); out.writeUTF("minecraft:overworld");
            out.writeInt(1); out.writeInt(70); out.writeInt(2); out.writeLong(123);
            out.writeBoolean(true); out.writeBoolean(true);
        }
        DeathWaypointCache.openWorld(dir, "new-stable-path", "legacy");
        var restored = DeathWaypointCache.merge(List.of());
        check(restored.size() == 1, "legacy recovered flag still hides death");
        check(DeathWaypointCache.shouldRenderDeathMarker(restored.get(0)), "grave heuristic hides death");
        DeathWaypointCache.disconnect();
        DeathWaypointCache.openWorld(dir, "new-stable-path", null);
        check(DeathWaypointCache.merge(List.of()).size() == 1, "migration not persisted");
        DeathWaypointCache.disconnect();
        System.out.println("PASS: dimension travel, reconnect, disk reload, world isolation, duplicates, repeat deaths, V1 migration");
    }
}

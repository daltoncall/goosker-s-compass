package com.goosker.compass.client.hud;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Client-side, per-world disk cache for vanilla and Xaero death coordinates.
 * The 10k fade distance affects rendering ONLY. It never evicts cached deaths.
 * World identity is bound once per connection, never to the active dimension.
 * Grave visibility and previously inferred recovery flags never delete a death.
 */
public final class DeathWaypointCache {
    private static final String MAGIC = "GSC_DEATH_CACHE_V1";
    private static final int MAX_DEATHS = 256;
    private static final Map<String, DeathEntry> entries = new LinkedHashMap<>();
    private static String worldKey;
    private static Path file;
    private static boolean loadFailed;
    private static boolean dirty;

    private DeathWaypointCache() {}

    /** Called by MarkerManager on its existing 10-tick refresh, not every frame. */
    public static synchronized List<CompassMarker> merge(List<CompassMarker> liveMarkers) {
        if (worldKey == null) return List.copyOf(liveMarkers);
        boolean changed = false;
        List<CompassMarker> results = new ArrayList<>();
        Set<String> emitted = new HashSet<>();

        for (CompassMarker marker : liveMarkers) {
            if (marker.type() != MarkerType.DEATH) {
                results.add(marker);
                continue;
            }
            String id = id(marker);
            DeathEntry existing = entries.get(id);
            boolean duplicate = entries.values().stream().anyMatch(e ->
                    samePosition(e.marker, marker.dimensionId(), marker.x(), marker.y(), marker.z())
                    && Math.abs(e.marker.createdAt() - marker.createdAt()) < 30000);
            if (existing == null && !duplicate) {
                entries.put(id, new DeathEntry(marker, false, false));
                changed = true;
            }
        }
        // Crucially, use the persisted set even when Xaero stops advertising a
        // death because of distance, dimension changes, or reload timing.
        for (Map.Entry<String, DeathEntry> e : entries.entrySet()) {
            DeathEntry state = e.getValue();
            if (emitted.add(e.getKey())) results.add(state.marker);
        }
        if (changed) dirty = true;
        trim();
        save();
        return List.copyOf(results);
    }

    /** Deaths are retained independently of grave/chunk visibility. */
    public static synchronized boolean shouldRenderDeathMarker(CompassMarker marker) {
        return true;
    }

    public static synchronized void openWorld(Path configDir, String key, String legacyKey) {
        if (key.equals(worldKey)) return;
        disconnect();
        worldKey = key;
        try {
            file = configDir.resolve("gooskers-compass/death-cache/" + hexDigest(key) + ".bin");
            if (Files.isRegularFile(file)) read();
            else if (legacyKey != null) {
                Path destination = file;
                file = configDir.resolve("gooskers-compass/death-cache/" + hexDigest(legacyKey) + ".bin");
                if (Files.isRegularFile(file)) read();
                file = destination;
                dirty = !entries.isEmpty();
            }
        } catch (Exception e) {
            loadFailed = true; // Preserve an unreadable original instead of overwriting it.
            System.err.println("[Goosker's Compass] Could not read death cache: " + e);
        }
        save();
    }

    public static synchronized void disconnect() {
        save();
        entries.clear();
        worldKey = null;
        file = null;
        loadFailed = false;
        dirty = false;
    }

    /** Capture vanilla's actual dimension, independently of Xaero's active set. */
    public static synchronized void rememberDeath(String dimension, int x, int y, int z, boolean newDeath) {
        if (worldKey == null) return;
        if (!newDeath && entries.values().stream().anyMatch(e ->
                samePosition(e.marker, dimension, x, y, z))) return;
        long newest = entries.values().stream().mapToLong(e -> e.marker.createdAt()).max().orElse(0L);
        long created = Math.max(System.currentTimeMillis(), newest + 1);
        CompassMarker marker = new CompassMarker(MarkerType.DEATH, "Death", x, y, z, dimension, created, null);
        entries.put(id(marker), new DeathEntry(marker, false, false));
        dirty = true;
        trim();
        save();
    }

    private static boolean samePosition(CompassMarker m, String dimension, int x, int y, int z) {
        return dimension.equals(m.dimensionId()) && m.x() == x && m.y() == y && m.z() == z;
    }

    private static void trim() {
        if (entries.size() <= MAX_DEATHS) return;
        List<DeathEntry> sorted = new ArrayList<>(entries.values());
        sorted.sort(Comparator.comparingLong((DeathEntry e) -> e.marker.createdAt()).reversed());
        entries.clear();
        for (DeathEntry e : sorted.subList(0, MAX_DEATHS)) entries.put(id(e.marker), e);
        dirty = true;
    }

    static String id(CompassMarker marker) {
        // Timestamp separates multiple deaths at the same coordinates.
        return marker.dimensionId() + "|" + marker.x() + "|" + marker.y() + "|" + marker.z()
                + "|" + marker.createdAt();
    }

    private static void read() throws Exception {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            if (!MAGIC.equals(in.readUTF())) throw new IllegalStateException("Unknown death cache format");
            int count = in.readInt();
            if (count < 0 || count > MAX_DEATHS) throw new IllegalStateException("Invalid cache entry count");
            for (int i = 0; i < count; i++) {
                String name = in.readUTF();
                String dim = in.readUTF();
                int x = in.readInt(), y = in.readInt(), z = in.readInt();
                long created = in.readLong();
                boolean observed = in.readBoolean(), recovered = in.readBoolean();
                CompassMarker marker = new CompassMarker(MarkerType.DEATH, name, x, y, z, dim, created, null);
                entries.put(id(marker), new DeathEntry(marker, false, false));
            }
        }
    }

    private static void save() {
        if (!dirty || loadFailed || file == null) return;
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(tmp)))) {
                out.writeUTF(MAGIC);
                out.writeInt(entries.size());
                for (DeathEntry state : entries.values()) {
                    CompassMarker marker = state.marker;
                    out.writeUTF(safeUtf(marker.name()));
                    out.writeUTF(safeUtf(marker.dimensionId()));
                    out.writeInt(marker.x()); out.writeInt(marker.y()); out.writeInt(marker.z());
                    out.writeLong(marker.createdAt());
                    out.writeBoolean(state.observed); out.writeBoolean(state.recovered);
                }
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
        } catch (Exception e) {
            System.err.println("[Goosker's Compass] Could not save death waypoint cache: " + e);
        }
    }

    private static String safeUtf(String s) {
        if (s == null) return "";
        return s.length() > 10000 ? s.substring(0, 10000) : s;
    }

    private static String hexDigest(String s) throws Exception {
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder b = new StringBuilder();
        for (byte v : bytes) b.append(String.format("%02x", v & 255));
        return b.toString();
    }

    private record DeathEntry(CompassMarker marker, boolean observed, boolean recovered) {}
}

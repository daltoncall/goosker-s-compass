package com.goosker.compass.client.hud;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
 * Client-side, per-world disk cache for Xaero death waypoint coordinates.
 * The 10k fade distance affects rendering ONLY. It never evicts cached deaths.
 * Fail-open: if a world cannot be safely identified, no cross-world caching occurs.
 *
 * This class deliberately uses only the JDK and the existing CompassMarker record;
 * the reflective MC calls are intermediary names used by Minecraft 1.20.1.
 */
public final class DeathWaypointCache {
    private static final String MAGIC = "GSC_DEATH_CACHE_V1";
    private static final String GRAVE_ENTITY = "me.mgin.graves.block.entity.GraveBlockEntity";
    private static final int MAX_DEATHS = 256;
    private static final int GRAVE_RADIUS = 16;
    private static final int GRAVE_Y_RADIUS = 24;
    private static final long CHECK_INTERVAL_MS = 1000;
    private static final Map<String, DeathEntry> entries = new LinkedHashMap<>();
    private static final Map<String, Long> checkedAt = new LinkedHashMap<>();
    private static String worldKey;
    private static Path file;
    private static boolean loadFailed;

    private DeathWaypointCache() {}

    /** Called by MarkerManager on its existing 10-tick refresh, not every frame. */
    public static synchronized List<CompassMarker> merge(List<CompassMarker> liveMarkers) {
        String key = determineWorldKey();
        if (key == null || key.isBlank()) return List.copyOf(liveMarkers);
        switchWorld(key);
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
            if (existing == null) {
                entries.put(id, new DeathEntry(marker, false, false));
                changed = true;
            }
        }
        // Crucially, use the persisted set even when Xaero stops advertising a
        // death because of distance, dimension changes, or reload timing.
        for (Map.Entry<String, DeathEntry> e : entries.entrySet()) {
            DeathEntry state = e.getValue();
            if (!state.recovered && emitted.add(e.getKey())) results.add(state.marker);
        }
        if (entries.size() > MAX_DEATHS) {
            List<Map.Entry<String, DeathEntry>> sorted = new ArrayList<>(entries.entrySet());
            sorted.sort(Comparator.comparingLong((Map.Entry<String, DeathEntry> e) -> e.getValue().marker.createdAt()).reversed());
            Set<String> keep = new HashSet<>();
            for (int i = 0; i < Math.min(sorted.size(), MAX_DEATHS); i++) keep.add(sorted.get(i).getKey());
            entries.keySet().removeIf(s -> !keep.contains(s));
            changed = true;
        }
        if (changed) save();
        return List.copyOf(results);
    }

    /**
     * Only mark a death as recovered after its actual grave was *observed* nearby
     * and later disappeared while the relevant chunks were loaded. Merely having
     * no grave at the exact Xaero death coordinate is NOT proof of recovery.
     */
    public static synchronized boolean shouldRenderDeathMarker(CompassMarker marker) {
        if (!isModLoaded("forgottengraves")) return true;
        if (worldKey == null) return true;
        DeathEntry state = entries.get(id(marker));
        if (state == null) return true;
        if (state.recovered) return false;
        long now = System.currentTimeMillis();
        Long previous = checkedAt.get(id(marker));
        if (previous != null && now - previous < CHECK_INTERVAL_MS) return true;
        checkedAt.put(id(marker), now);
        Boolean nearby = isGraveNearbyIfLoaded(marker);
        if (nearby == null) return true; // chunks not fully loaded: no conclusion.
        if (nearby) {
            if (!state.observed) {
                entries.put(id(marker), new DeathEntry(state.marker, true, false));
                save();
            }
        } else if (state.observed) {
            entries.put(id(marker), new DeathEntry(state.marker, true, true));
            save();
            return false;
        }
        return true;
    }

    static String id(CompassMarker marker) {
        // Timestamp separates multiple deaths at the same coordinates.
        return marker.dimensionId() + "|" + marker.x() + "|" + marker.y() + "|" + marker.z()
                + "|" + marker.createdAt();
    }

    private static void switchWorld(String key) {
        if (key.equals(worldKey)) return;
        entries.clear();
        checkedAt.clear();
        worldKey = key;
        loadFailed = false;
        try {
            Class<?> loader = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object instance = loader.getMethod("getInstance").invoke(null);
            Path configDir = (Path) loader.getMethod("getConfigDir").invoke(instance);
            file = configDir.resolve("gooskers-compass/death-cache/" + hexDigest(key) + ".bin");
            if (Files.isRegularFile(file)) read();
        } catch (Exception e) {
            file = null;
            loadFailed = true;
            System.err.println("[Goosker's Compass] Death cache read disabled: " + e);
        }
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
                entries.put(id(marker), new DeathEntry(marker, observed, recovered));
            }
        }
    }

    private static void save() {
        if (loadFailed || file == null) return;
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

    private static boolean isModLoaded(String modId) {
        try {
            Class<?> loader = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object instance = loader.getMethod("getInstance").invoke(null);
            return (boolean) loader.getMethod("isModLoaded", String.class).invoke(instance, modId);
        } catch (Exception e) { return false; }
    }

    private static String determineWorldKey() {
        try {
            Class<?> mcClass = Class.forName("net.minecraft.class_310");
            Object mc = mcClass.getMethod("method_1551").invoke(null);
            Object serverInfo = mcClass.getMethod("method_1558").invoke(mc);
            if (serverInfo != null) {
                Field addressField = serverInfo.getClass().getField("field_3761");
                String address = String.valueOf(addressField.get(serverInfo));
                if (address == null || address.equals("null") || address.isBlank()) return null;
                // A matching Utility installation can turn rotating e4mc hosts
                // into a stable ID. Never collapse unrelated e4mc worlds by name.
                try {
                    Class<?> identity = Class.forName("com.goosker.xaeroworlds.WorldIdentity");
                    Object resolved = identity.getMethod("resolve", String.class).invoke(null, address);
                    if (resolved instanceof String s && !s.isBlank()) address = s;
                } catch (ReflectiveOperationException | LinkageError ignored) { }
                return "server:" + address.trim().toLowerCase(java.util.Locale.ROOT);
            }
            Object server = mcClass.getMethod("method_1576").invoke(mc);
            if (server == null) return null;
            Object props = server.getClass().getMethod("method_27728").invoke(server);
            String name = String.valueOf(props.getClass().getMethod("method_150").invoke(props));
            return name.isBlank() ? null : "singleplayer:" + name;
        } catch (Exception e) {
            return null;
        }
    }

    /** null = no reliable observation (one or more relevant chunks unloaded). */
    private static Boolean isGraveNearbyIfLoaded(CompassMarker marker) {
        try {
            Class<?> mcClass = Class.forName("net.minecraft.class_310");
            Object mc = mcClass.getMethod("method_1551").invoke(null);
            Object world = mcClass.getField("field_1687").get(mc);
            if (world == null) return null;
            Class<?> posType = Class.forName("net.minecraft.class_2338");
            Object markerPos = posType.getConstructor(int.class, int.class, int.class)
                    .newInstance(marker.x(), marker.y(), marker.z());
            Method loaded = world.getClass().getMethod("method_22340", posType);
            if (!(boolean) loaded.invoke(world, markerPos)) return null;
            Method chunkGetter = world.getClass().getMethod("method_8497", int.class, int.class);
            Method getX = posType.getMethod("method_10263");
            Method getY = posType.getMethod("method_10264");
            Method getZ = posType.getMethod("method_10260");
            boolean allLoaded = true;
            for (int cx = (marker.x() - GRAVE_RADIUS) >> 4; cx <= (marker.x() + GRAVE_RADIUS) >> 4; cx++) {
                for (int cz = (marker.z() - GRAVE_RADIUS) >> 4; cz <= (marker.z() + GRAVE_RADIUS) >> 4; cz++) {
                    Object checkPos = posType.getConstructor(int.class, int.class, int.class)
                            .newInstance(cx << 4, marker.y(), cz << 4);
                    if (!(boolean) loaded.invoke(world, checkPos)) {
                        allLoaded = false;
                        continue;
                    }
                    Object chunk = chunkGetter.invoke(world, cx, cz);
                    if (chunk == null) { allLoaded = false; continue; }
                    Object raw = chunk.getClass().getMethod("method_12214").invoke(chunk);
                    if (!(raw instanceof Map<?, ?> blockEntities)) continue;
                    for (Map.Entry<?, ?> entry : blockEntities.entrySet()) {
                        Object be = entry.getValue();
                        if (be == null) continue;
                        boolean isGrave = false;
                        for (Class<?> type = be.getClass(); type != null; type = type.getSuperclass()) {
                            if (GRAVE_ENTITY.equals(type.getName())) { isGrave = true; break; }
                        }
                        if (!isGrave) continue;
                        Object p = entry.getKey();
                        int x = ((Number) getX.invoke(p)).intValue();
                        int y = ((Number) getY.invoke(p)).intValue();
                        int z = ((Number) getZ.invoke(p)).intValue();
                        if (Math.abs(x - marker.x()) <= GRAVE_RADIUS &&
                                Math.abs(y - marker.y()) <= GRAVE_Y_RADIUS &&
                                Math.abs(z - marker.z()) <= GRAVE_RADIUS) return true;
                    }
                }
            }
            return allLoaded ? Boolean.FALSE : null;
        } catch (Exception e) {
            // Failure cannot be interpreted as an already collected grave.
            return null;
        }
    }

    private record DeathEntry(CompassMarker marker, boolean observed, boolean recovered) {}
}

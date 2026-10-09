package com.goosker.compass.client.compat;

import com.goosker.compass.GooskersCompass;
import com.goosker.compass.client.hud.CompassMarker;
import com.goosker.compass.client.hud.MarkerType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class XaeroCompat {
    private static boolean warned;

    private XaeroCompat() {}

    public static boolean loaded() {
        return FabricLoader.getInstance().isModLoaded("xaerominimap");
    }

    public static List<CompassMarker> collect(MinecraftClient client, Set<UUID> knownWaystoneIds) {
        if (!loaded() || client.world == null) {
            return List.of();
        }
        try {
            Object manager = findWaypointsManager();
            if (manager == null) {
                return List.of();
            }
            String dimension = client.world.getRegistryKey().getValue().toString();
            List<CompassMarker> markers = new ArrayList<>();
            Set<String> dedupe = new HashSet<>();

            Object currentWorld = ReflectionUtil.invoke(manager, "getCurrentWorld");
            if (currentWorld != null) {
                Object set = ReflectionUtil.invoke(currentWorld, "getCurrentSet");
                if (set != null) {
                    Object waypointList = ReflectionUtil.hasMethod(set, "getList", 0)
                            ? ReflectionUtil.invoke(set, "getList")
                            : ReflectionUtil.invoke(set, "getWaypoints");
                    if (waypointList instanceof Iterable<?> iterable) {
                        for (Object waypoint : iterable) {
                            addWaypoint(markers, dedupe, waypoint, dimension, false, knownWaystoneIds);
                        }
                    }
                }
            }

            Object serverList = ReflectionUtil.invoke(manager, "getServerWaypoints");
            if (serverList instanceof Iterable<?> iterable) {
                for (Object waypoint : iterable) {
                    addWaypoint(markers, dedupe, waypoint, dimension, true, knownWaystoneIds);
                }
            }

            warned = false;
            return markers;
        } catch (Throwable t) {
            if (!warned) {
                GooskersCompass.LOGGER.warn("Xaero's Minimap is installed, but its waypoint data could not be read.", t);
                warned = true;
            }
            return List.of();
        }
    }

    private static Object findWaypointsManager() throws ReflectiveOperationException {
        Object hudMod = ReflectionUtil.staticField("xaero.common.HudMod", "INSTANCE");
        if (hudMod == null) {
            return null;
        }
        Object hud = ReflectionUtil.invoke(hudMod, "getHud");
        if (hud == null) {
            return null;
        }
        Object moduleManager = ReflectionUtil.invoke(hud, "getModuleManager");
        Object modules = ReflectionUtil.invoke(moduleManager, "getModules");
        if (!(modules instanceof Iterable<?> iterable)) {
            return null;
        }
        for (Object module : iterable) {
            Object session = ReflectionUtil.invoke(module, "getCurrentSession");
            if (session != null && hasClassInHierarchy(session.getClass(), "xaero.common.minimap.waypoints.WaypointsManager")) {
                return session;
            }
        }
        return null;
    }

    private static boolean hasClassInHierarchy(Class<?> type, String name) {
        Class<?> cursor = type;
        while (cursor != null) {
            if (cursor.getName().equals(name)) {
                return true;
            }
            cursor = cursor.getSuperclass();
        }
        return false;
    }

    private static void addWaypoint(List<CompassMarker> out, Set<String> dedupe, Object waypoint,
                                    String dimension, boolean forceServer, Set<UUID> knownWaystoneIds) throws ReflectiveOperationException {
        if (Boolean.TRUE.equals(ReflectionUtil.invoke(waypoint, "isDisabled"))) {
            return;
        }

        UUID waystoneId = readXmxwWaystoneId(waypoint);
        if (waystoneId != null && knownWaystoneIds.contains(waystoneId)) {
            return;
        }

        int x = ((Number) ReflectionUtil.invoke(waypoint, "getX")).intValue();
        int y = ((Number) ReflectionUtil.invoke(waypoint, "getY")).intValue();
        int z = ((Number) ReflectionUtil.invoke(waypoint, "getZ")).intValue();
        String name = String.valueOf(ReflectionUtil.invoke(waypoint, "getName"));
        Object purpose = ReflectionUtil.invoke(waypoint, "getPurpose");
        String purposeName = purpose == null ? "NORMAL" : purpose.toString();
        long createdAt = ((Number) ReflectionUtil.invoke(waypoint, "getCreatedAt")).longValue();

        MarkerType type;
        if (purposeName.equals("DEATH") || purposeName.equals("OLD_DEATH")) {
            type = MarkerType.DEATH;
        } else {
            boolean server = forceServer;
            if (!server && ReflectionUtil.hasMethod(waypoint, "isServerWaypoint", 0)) {
                server = Boolean.TRUE.equals(ReflectionUtil.invoke(waypoint, "isServerWaypoint"));
            }
            type = server ? MarkerType.SERVER : MarkerType.NORMAL;
        }

        String key = type + ":" + x + ":" + y + ":" + z + ":" + name;
        if (dedupe.add(key)) {
            out.add(new CompassMarker(type, name, x, y, z, dimension, createdAt, waystoneId));
        }
    }

    private static UUID readXmxwWaystoneId(Object waypoint) {
        if (!FabricLoader.getInstance().isModLoaded("xmxw") || !ReflectionUtil.hasMethod(waypoint, "xmxw$getWaystoneId", 0)) {
            return null;
        }
        try {
            Object result = ReflectionUtil.invoke(waypoint, "xmxw$getWaystoneId");
            return result instanceof UUID uuid ? uuid : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}

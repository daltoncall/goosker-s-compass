package com.goosker.compass.client.compat;

import com.goosker.compass.GooskersCompass;
import com.goosker.compass.client.hud.CompassMarker;
import com.goosker.compass.client.hud.MarkerType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class WaystonesCompat {
    private static boolean warned;

    private WaystonesCompat() {}

    public static boolean loaded() {
        return FabricLoader.getInstance().isModLoaded("waystones");
    }

    public static List<CompassMarker> collect(MinecraftClient client) {
        if (!loaded() || client.player == null) {
            return List.of();
        }
        try {
            Class<?> api = Class.forName("net.blay09.mods.waystones.api.WaystonesAPI");
            Method getter = ReflectionUtil.findMethod(api, "getActivatedWaystones", 1);
            Object result = getter.invoke(null, client.player);
            if (!(result instanceof Collection<?> waystones)) {
                return List.of();
            }

            List<CompassMarker> markers = new ArrayList<>();
            for (Object waystone : waystones) {
                UUID id = (UUID) ReflectionUtil.invoke(waystone, "getWaystoneUid");
                String name = String.valueOf(ReflectionUtil.invoke(waystone, "getName"));
                BlockPos pos = (BlockPos) ReflectionUtil.invoke(waystone, "getPos");
                @SuppressWarnings("unchecked")
                RegistryKey<World> dimension = (RegistryKey<World>) ReflectionUtil.invoke(waystone, "getDimension");
                boolean valid = !ReflectionUtil.hasMethod(waystone, "isValid", 0)
                        || Boolean.TRUE.equals(ReflectionUtil.invoke(waystone, "isValid"));
                if (!valid || pos == null || dimension == null) {
                    continue;
                }
                markers.add(new CompassMarker(
                        MarkerType.WAYSTONE,
                        name,
                        pos.getX(), pos.getY(), pos.getZ(),
                        dimension.getValue().toString(),
                        0L,
                        id
                ));
            }
            warned = false;
            if (markers.isEmpty()) {
                List<CompassMarker> xmxwMarkers = collectFromXmxw(client);
                if (!xmxwMarkers.isEmpty()) {
                    return xmxwMarkers;
                }
            }
            return markers;
        } catch (Throwable t) {
            if (!warned) {
                GooskersCompass.LOGGER.warn("Waystones integration is present but could not be read. XMXW fallback will still be attempted if available.", t);
                warned = true;
            }
            return collectFromXmxw(client);
        }
    }

    private static List<CompassMarker> collectFromXmxw(MinecraftClient client) {
        if (!FabricLoader.getInstance().isModLoaded("xmxw") || client.world == null) {
            return List.of();
        }
        try {
            Object mapObj = ReflectionUtil.staticField("dev.alinco8.xmxw.XMXWClient", "waystones");
            if (!(mapObj instanceof java.util.Map<?, ?> map)) {
                return List.of();
            }
            Object listObj = map.get(client.world.getRegistryKey().getValue());
            if (!(listObj instanceof Iterable<?> iterable)) {
                return List.of();
            }
            String dimension = client.world.getRegistryKey().getValue().toString();
            List<CompassMarker> markers = new ArrayList<>();
            for (Object data : iterable) {
                int x = ((Number) ReflectionUtil.invoke(data, "getX")).intValue();
                int y = ((Number) ReflectionUtil.invoke(data, "getY")).intValue();
                int z = ((Number) ReflectionUtil.invoke(data, "getZ")).intValue();
                String name = String.valueOf(ReflectionUtil.invoke(data, "getName"));
                UUID id = (UUID) ReflectionUtil.invoke(data, "getId");
                markers.add(new CompassMarker(MarkerType.WAYSTONE, name, x, y, z, dimension, 0L, id));
            }
            return markers;
        } catch (Throwable ignored) {
            return List.of();
        }
    }
}

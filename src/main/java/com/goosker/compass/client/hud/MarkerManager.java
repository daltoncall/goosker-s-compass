package com.goosker.compass.client.hud;

import com.goosker.compass.client.compat.WaystonesCompat;
import com.goosker.compass.client.compat.XaeroCompat;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class MarkerManager {
    private static volatile List<CompassMarker> cached = List.of();
    private static int ticks;

    private MarkerManager() {}

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            clear();
            return;
        }
        ticks++;
        if (ticks < 10) {
            return;
        }
        ticks = 0;
        refresh(client);
    }

    public static void clear() {
        cached = List.of();
        ticks = 0;
    }

    public static List<CompassMarker> markers() {
        return cached;
    }

    private static void refresh(MinecraftClient client) {
        List<CompassMarker> result = new ArrayList<>();
        Set<String> dedupe = new HashSet<>();

        List<CompassMarker> waystones = WaystonesCompat.collect(client);
        Set<UUID> waystoneIds = new HashSet<>();
        for (CompassMarker marker : waystones) {
            if (marker.sourceId() != null) waystoneIds.add(marker.sourceId());
            if (dedupe.add(marker.dedupeKey())) result.add(marker);
        }

        for (CompassMarker marker : XaeroCompat.collect(client, waystoneIds)) {
            if (dedupe.add(marker.dedupeKey())) result.add(marker);
        }

        cached = List.copyOf(result);
    }
}

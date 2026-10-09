package com.goosker.compass.client.hud;

import java.util.UUID;

public record CompassMarker(
        MarkerType type,
        String name,
        int x,
        int y,
        int z,
        String dimensionId,
        long createdAt,
        UUID sourceId
) {
    public String dedupeKey() {
        if (sourceId != null) {
            return type + ":" + sourceId;
        }
        return type + ":" + x + ":" + y + ":" + z + ":" + name;
    }
}

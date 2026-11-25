package com.goosker.compass.client.hud;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.WorldSavePath;

/** Runs every tick, including with the compass hidden or Xaero unavailable. */
public final class DeathTracker {
    private static boolean bound;
    private static boolean wasDead;

    private DeathTracker() {}

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (!bound && !bind(client)) return;
        boolean dead = client.player.isDead();
        if (dead && !wasDead) {
            var pos = client.player.getBlockPos();
            DeathWaypointCache.rememberDeath(client.world.getRegistryKey().getValue().toString(),
                    pos.getX(), pos.getY(), pos.getZ(), true);
        }
        wasDead = dead;
        // The server syncs this even after respawn and on reconnect. An empty
        // value during a dimension transition never clears the persisted point.
        client.player.getLastDeathPos().ifPresent(death -> {
            var pos = death.getPos();
            DeathWaypointCache.rememberDeath(death.getDimension().getValue().toString(),
                    pos.getX(), pos.getY(), pos.getZ(), false);
        });
    }

    private static boolean bind(MinecraftClient client) {
        String key;
        String legacy = null;
        if (client.getServer() != null) {
            key = "singleplayer-path:" + client.getServer().getSavePath(WorldSavePath.ROOT)
                    .toAbsolutePath().normalize();
            legacy = "singleplayer:" + client.getServer().getSaveProperties().getLevelName();
        } else if (client.getCurrentServerEntry() != null) {
            String address = client.getCurrentServerEntry().address;
            if (address == null || address.isBlank()) return false;
            try {
                Class<?> identity = Class.forName("com.goosker.xaeroworlds.WorldIdentity");
                Object resolved = identity.getMethod("resolve", String.class).invoke(null, address);
                if (resolved instanceof String s && !s.isBlank()) address = s;
            } catch (ReflectiveOperationException | LinkageError ignored) { }
            key = "server:" + address.trim().toLowerCase(java.util.Locale.ROOT);
        } else return false;
        DeathWaypointCache.openWorld(FabricLoader.getInstance().getConfigDir(), key, legacy);
        bound = true;
        return true;
    }

    public static void disconnect() {
        DeathWaypointCache.disconnect();
        bound = false;
        wasDead = false;
    }
}

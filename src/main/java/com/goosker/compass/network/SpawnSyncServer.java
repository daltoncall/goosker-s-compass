package com.goosker.compass.network;

import com.goosker.compass.GooskersCompass;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SpawnSyncServer {
    public static final Identifier PACKET_ID = new Identifier(GooskersCompass.MOD_ID, "spawn_sync");
    private static final Map<UUID, SpawnSnapshot> LAST_SENT = new HashMap<>();
    private static int tickCounter;

    private SpawnSyncServer() {}

    public static void initialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendIfChanged(handler.player, true));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LAST_SENT.remove(handler.player.getUuid()));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter < 20) {
                return;
            }
            tickCounter = 0;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                sendIfChanged(player, false);
            }
        });
    }

    private static void sendIfChanged(ServerPlayerEntity player, boolean force) {
        if (!ServerPlayNetworking.canSend(player, PACKET_ID)) {
            return;
        }

        // getSpawnPointPosition() is null until the player explicitly sets a respawn
        // point (bed/respawn anchor). Do not substitute the default world spawn here.
        BlockPos pos = player.getSpawnPointPosition();
        String dimension = pos == null ? null : player.getSpawnPointDimension().getValue().toString();
        SpawnSnapshot current = new SpawnSnapshot(dimension, pos == null ? null : pos.toImmutable());
        SpawnSnapshot old = LAST_SENT.get(player.getUuid());
        if (!force && current.equals(old)) {
            return;
        }

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(pos != null);
        if (pos != null) {
            buf.writeString(dimension);
            buf.writeBlockPos(pos);
        }
        ServerPlayNetworking.send(player, PACKET_ID, buf);
        LAST_SENT.put(player.getUuid(), current);
    }

    private record SpawnSnapshot(String dimension, BlockPos position) {}
}

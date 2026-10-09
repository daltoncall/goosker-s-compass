package com.goosker.compass.client;

import com.goosker.compass.client.config.ConfigManager;
import com.goosker.compass.client.hud.CompassHudRenderer;
import com.goosker.compass.client.hud.MarkerManager;
import com.goosker.compass.network.SpawnState;
import com.goosker.compass.network.SpawnSyncServer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.util.math.BlockPos;

public final class GooskersCompassClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ConfigManager.load();

        ClientPlayNetworking.registerGlobalReceiver(SpawnSyncServer.PACKET_ID, (client, handler, buf, responseSender) -> {
            boolean hasSpawn = buf.readBoolean();
            String dimension = hasSpawn ? buf.readString() : null;
            BlockPos pos = hasSpawn ? buf.readBlockPos() : null;
            client.execute(() -> {
                if (hasSpawn) {
                    SpawnState.set(dimension, pos);
                } else {
                    SpawnState.clear();
                }
            });
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            SpawnState.clear();
            MarkerManager.clear();
        });

        ClientTickEvents.END_CLIENT_TICK.register(MarkerManager::tick);
        HudRenderCallback.EVENT.register(CompassHudRenderer::render);
    }
}

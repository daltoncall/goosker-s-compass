package com.goosker.compass;

import com.goosker.compass.network.SpawnSyncServer;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GooskersCompass implements ModInitializer {
    public static final String MOD_ID = "gooskers_compass";
    public static final Logger LOGGER = LoggerFactory.getLogger("Goosker's Compass");

    @Override
    public void onInitialize() {
        SpawnSyncServer.initialize();
    }
}

package com.goosker.compass.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.goosker.compass.GooskersCompass;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("gooskers-compass.json");
    private static CompassConfig config = new CompassConfig();

    private ConfigManager() {}

    public static CompassConfig get() {
        return config;
    }

    public static void set(CompassConfig newConfig) {
        newConfig.sanitize();
        config = newConfig;
    }

    public static void load() {
        if (!Files.exists(PATH)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(PATH)) {
            CompassConfig loaded = GSON.fromJson(reader, CompassConfig.class);
            if (loaded != null) {
                loaded.sanitize();
                config = loaded;
            }
        } catch (Exception e) {
            GooskersCompass.LOGGER.warn("Could not read compass config; using defaults.", e);
        }
    }

    public static void save() {
        config.sanitize();
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (Exception e) {
            GooskersCompass.LOGGER.error("Could not save compass config.", e);
        }
    }
}

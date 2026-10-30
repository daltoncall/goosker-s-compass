package com.goosker.compass.client.hud;

import com.goosker.compass.client.config.CompassConfig;
import com.goosker.compass.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;

/** Vertical displacement of the entire vanilla boss-bar renderer, including titles. */
public final class BossBarPositioning {
    private BossBarPositioning() {}

    public static int verticalOffset() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null || client.options.hudHidden) {
            return 0;
        }
        CompassConfig config = ConfigManager.get();
        if (!config.enabled || !config.bossBarsBelowCompass ||
                (config.hideWithDebugHud && client.options.debugEnabled)) {
            return 0;
        }
        // Vanilla boss-bar title starts at y=3. The compass caps end at y+23,
        // and its optional focused label ends at about y+36.
        int compassBottom = (int) Math.ceil((config.hudY +
                (config.showLabels ? 36 : 23)) * config.hudScale);
        return Math.max(0, compassBottom + config.bossBarGap - 3);
    }
}

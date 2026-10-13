package com.goosker.compass.client.hud;

import com.goosker.compass.GooskersCompass;
import com.goosker.compass.client.config.CompassConfig;
import com.goosker.compass.client.config.ConfigManager;
import com.goosker.compass.network.SpawnState;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CompassHudRenderer {
    private static final Identifier ICON_WAYPOINT = id("textures/gui/marker_waypoint.png");
    private static final Identifier ICON_WAYSTONE = id("textures/gui/marker_waystone.png");
    private static final Identifier ICON_DEATH = id("textures/gui/marker_death.png");
    private static final Identifier ICON_DEATH_DECAYED = id("textures/gui/marker_death_decayed.png");
    private static final Identifier ICON_DEATH_FULLY_DECAYED = id("textures/gui/marker_death_fully_decayed.png");
    private static final Identifier ICON_SPAWN = id("textures/gui/marker_spawn.png");
    private static final Identifier CAP_LEFT = id("textures/gui/cap_left.png");
    private static final Identifier CAP_RIGHT = id("textures/gui/cap_right.png");

    private static final int ICON_SIZE = 20;
    private static final int CAP_SIZE = 19;
    private static final String FORGOTTEN_GRAVES_BLOCK_ENTITY = "me.mgin.graves.block.entity.GraveBlockEntity";

    private CompassHudRenderer() {}

    public static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || client.options.hudHidden) return;

        CompassConfig config = ConfigManager.get();
        if (!config.enabled || (config.hideWithDebugHud && client.options.debugEnabled)) return;

        float scale = (float) config.hudScale;
        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, 1.0f);
        try {
            int screenWidth = Math.round(client.getWindow().getScaledWidth() / scale);
            int width = Math.min(config.hudWidth, Math.max(120, screenWidth - 20));
            int centerX = screenWidth / 2;
            int left = centerX - width / 2;
            int y = config.hudY;
            float yaw = player.getYaw(tickDelta);

            drawFrame(context, left, y, width, config);
            drawHeading(context, client.textRenderer, centerX, left, y, width, yaw, config);

            List<VisibleMarker> visible = collectVisibleMarkers(player, yaw, centerX, width, config);
            visible.sort(Comparator.comparingInt(v -> v.marker.type().priority()));
            for (VisibleMarker marker : visible) drawMarker(context, marker, y);
            drawFocusedLabel(context, client.textRenderer, visible, centerX, y, config);
        } finally {
            context.getMatrices().pop();
        }
    }

    private static void drawFrame(DrawContext context, int left, int y, int width, CompassConfig config) {
        int top = y + 7;
        int bottom = y + 20;
        int alpha = (int) Math.round(config.backgroundOpacity * 255.0);
        alpha = Math.max(alpha, 46);

        int bg = (alpha << 24) | 0x111316;
        int edge = 0xC07F858B;
        int inner = 0x80464B50;
        int arrow = 0xD09AA0A6;

        context.fill(left, top, left + width, bottom, bg);
        context.fill(left, top, left + width, top + 1, edge);
        context.fill(left, bottom - 1, left + width, bottom, edge);
        context.fill(left, top + 2, left + width, top + 3, inner);
        context.fill(left, bottom - 3, left + width, bottom - 2, inner);

        int capY = y + 4;
        context.drawTexture(CAP_LEFT, left - CAP_SIZE, capY, 0.0f, 0.0f,
                CAP_SIZE, CAP_SIZE, CAP_SIZE, CAP_SIZE);
        context.drawTexture(CAP_RIGHT, left + width, capY, 0.0f, 0.0f,
                CAP_SIZE, CAP_SIZE, CAP_SIZE, CAP_SIZE);

        int center = left + width / 2;
        context.fill(center - 3, top - 4, center + 4, top - 3, arrow);
        context.fill(center - 2, top - 3, center + 3, top - 2, arrow);
        context.fill(center - 1, top - 2, center + 2, top - 1, arrow);
        context.fill(center, top - 1, center + 1, top, arrow);
    }

    private static void drawHeading(DrawContext context, TextRenderer textRenderer, int centerX, int left, int y,
                                    int width, float yaw, CompassConfig config) {
        double halfSpan = config.compassSpanDegrees / 2.0;

        if (config.showTicks) {
            for (int heading = -180; heading < 180; heading += 15) {
                double rel = wrapDegrees(heading - yaw);
                if (Math.abs(rel) > halfSpan) continue;
                if (config.showCardinals && Math.floorMod(heading, 90) == 0) continue;

                int x = angleToX(rel, centerX, width, halfSpan);
                boolean major = Math.floorMod(heading, 45) == 0;
                int h = major ? 4 : 2;
                context.fill(x, y + 12 - h / 2, x + 1, y + 12 + h / 2 + 1,
                        major ? 0xC0C1C5C8 : 0x806E7378);
            }
        }

        if (config.showCardinals) {
            drawCardinal(context, textRenderer, "N", 180, yaw, centerX, y, width, halfSpan);
            drawCardinal(context, textRenderer, "E", -90, yaw, centerX, y, width, halfSpan);
            drawCardinal(context, textRenderer, "S", 0, yaw, centerX, y, width, halfSpan);
            drawCardinal(context, textRenderer, "W", 90, yaw, centerX, y, width, halfSpan);
        }
    }

    private static void drawCardinal(DrawContext context, TextRenderer textRenderer, String label, double heading,
                                     float yaw, int centerX, int y, int width, double halfSpan) {
        double rel = wrapDegrees(heading - yaw);
        if (Math.abs(rel) > halfSpan) return;
        int x = angleToX(rel, centerX, width, halfSpan);
        int textWidth = textRenderer.getWidth(label);
        context.drawTextWithShadow(textRenderer, Text.literal(label), x - textWidth / 2, y + 9, 0xFFE2E4E5);
    }

    private static List<VisibleMarker> collectVisibleMarkers(ClientPlayerEntity player, float yaw, int centerX,
                                                              int width, CompassConfig config) {
        String currentDimension = player.getWorld().getRegistryKey().getValue().toString();
        List<CompassMarker> candidates = new ArrayList<>(MarkerManager.markers());

        if (config.showSpawn && SpawnState.hasCustomSpawn()) {
            BlockPos spawn = SpawnState.position();
            String spawnDimension = SpawnState.dimensionId();
            if (spawn != null && currentDimension.equals(spawnDimension)) {
                candidates.add(new CompassMarker(MarkerType.SPAWN, "Spawn", spawn.getX(), spawn.getY(), spawn.getZ(),
                        spawnDimension, 0L, null));
            }
        }

        List<CompassMarker> deaths = candidates.stream()
                .filter(m -> m.type() == MarkerType.DEATH)
                .sorted(Comparator.comparingLong(CompassMarker::createdAt).reversed())
                .limit(config.deathCount)
                .toList();

        double halfSpan = config.compassSpanDegrees / 2.0;
        List<VisibleMarker> visible = new ArrayList<>();
        for (CompassMarker marker : candidates) {
            if (!currentDimension.equals(marker.dimensionId())) continue;
            if (!enabled(marker.type(), config)) continue;
            if (marker.type() == MarkerType.DEATH && !deaths.contains(marker)) continue;

            double dx = marker.x() + 0.5 - player.getX();
            double dz = marker.z() + 0.5 - player.getZ();
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            double verticalDistance = Math.abs(marker.y() + 0.5 - player.getY());

            boolean ignoreRange = marker.type() == MarkerType.DEATH && config.deathsIgnoreRange;
            if (marker.type() == MarkerType.SPAWN) ignoreRange = true;
            if (!ignoreRange && (horizontalDistance > config.horizontalRange || verticalDistance > config.verticalRange)) {
                continue;
            }

            double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
            double relative = wrapDegrees(targetYaw - yaw);
            if (Math.abs(relative) > halfSpan) continue;

            int x = angleToX(relative, centerX, width, halfSpan);
            visible.add(new VisibleMarker(marker, x, relative, horizontalDistance));
        }
        return visible;
    }

    private static boolean enabled(MarkerType type, CompassConfig config) {
        return switch (type) {
            case NORMAL -> config.showNormalWaypoints;
            case SERVER -> config.showServerWaypoints;
            case WAYSTONE -> config.showWaystones;
            case SPAWN -> config.showSpawn;
            case DEATH -> config.showDeaths && config.deathCount > 0;
        };
    }

    private static float markerAlpha(MarkerType type, double distance, CompassConfig config) {
        boolean persistent = type == MarkerType.SPAWN || type == MarkerType.DEATH;
        double fadeDistance = persistent ? config.persistentFadeDistance : config.markerFadeDistance;
        double strength = persistent ? config.persistentFadeStrength : config.markerFadeStrength;

        if (distance >= fadeDistance) return 0.0f;
        if (distance <= 0.0) return 1.0f;
        double normalized = distance / fadeDistance;
        if (strength <= 0.0) return 1.0f;
        return (float) Math.pow(1.0 - normalized, strength);
    }

    private static void drawMarker(DrawContext context, VisibleMarker visible, int y) {
        context.getMatrices().push();
        // Markers render above cardinals/ticks so N/E/S/W disappear behind them cleanly.
        context.getMatrices().translate(0.0, 0.0, 100.0);
        try {
            CompassMarker marker = visible.marker;
            if (marker.type() == MarkerType.DEATH && !shouldRenderDeathMarker(marker)) return;

            float alpha = markerAlpha(marker.type(), visible.distance, ConfigManager.get());
            if (alpha <= 0.001f) return;

            Identifier texture = switch (marker.type()) {
                case NORMAL, SERVER -> ICON_WAYPOINT;
                case WAYSTONE -> ICON_WAYSTONE;
                case SPAWN -> ICON_SPAWN;
                case DEATH -> pickDeathIcon(marker);
            };

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            context.drawTexture(texture, visible.x - ICON_SIZE / 2, y + 1, 0.0f, 0.0f,
                    ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            context.getMatrices().pop();
        }
    }

    private static Identifier pickDeathIcon(CompassMarker marker) {
        int configuredLimit = Math.max(ConfigManager.get().deathCount, 1);
        int totalDeaths = 0;
        int newerDeaths = 0;
        long createdAt = marker.createdAt();

        for (CompassMarker candidate : MarkerManager.markers()) {
            if (candidate.type() != MarkerType.DEATH) continue;
            totalDeaths++;
            if (candidate.createdAt() > createdAt) newerDeaths++;
        }

        int visibleDeathCount = Math.min(totalDeaths, configuredLimit);
        if (visibleDeathCount <= 1) return ICON_DEATH;

        int bucket = (newerDeaths * 3) / visibleDeathCount;
        if (bucket <= 0) return ICON_DEATH;
        if (bucket == 1) return ICON_DEATH_DECAYED;
        return ICON_DEATH_FULLY_DECAYED;
    }

    private static boolean shouldRenderDeathMarker(CompassMarker marker) {
        if (!FabricLoader.getInstance().isModLoaded("forgottengraves")) return true;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return true;

        BlockPos pos = new BlockPos(marker.x(), marker.y(), marker.z());
        if (!client.world.isChunkLoaded(pos)) return true;

        BlockEntity blockEntity = client.world.getBlockEntity(pos);
        if (blockEntity == null) return false;
        return FORGOTTEN_GRAVES_BLOCK_ENTITY.equals(blockEntity.getClass().getName());
    }

    private static void drawFocusedLabel(DrawContext context, TextRenderer textRenderer, List<VisibleMarker> visible,
                                         int centerX, int y, CompassConfig config) {
        if (!config.showLabels || visible.isEmpty()) return;
        VisibleMarker focused = visible.stream()
                .filter(v -> Math.abs(v.relativeAngle) <= 15.0)
                .min(Comparator.comparingDouble(v -> Math.abs(v.relativeAngle)))
                .orElse(null);
        if (focused == null) return;

        String label = focused.marker.name();
        if (label == null || label.isBlank()) {
            label = switch (focused.marker.type()) {
                case DEATH -> "Death";
                case SPAWN -> "Spawn";
                case WAYSTONE -> "Waystone";
                case SERVER -> "Server Waypoint";
                case NORMAL -> "Waypoint";
            };
        }
        if (config.showDistances) label += "  " + Math.round(focused.distance) + "m";

        int maxWidth = 230;
        if (textRenderer.getWidth(label) > maxWidth) {
            label = textRenderer.trimToWidth(label, maxWidth - textRenderer.getWidth("…")) + "…";
        }
        int textWidth = textRenderer.getWidth(label);
        context.drawTextWithShadow(textRenderer, Text.literal(label), centerX - textWidth / 2, y + 26, 0xFFE5E5E5);
    }

    private static void drawPreviewIcon(DrawContext context, Identifier texture, int x, int y, float alpha) {
        if (alpha <= 0.001f) return;
        context.getMatrices().push();
        context.getMatrices().translate(0.0, 0.0, 100.0);
        try {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            context.drawTexture(texture, x - ICON_SIZE / 2, y + 1, 0.0f, 0.0f,
                    ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            context.getMatrices().pop();
        }
    }

    public static void renderConfigPreview(DrawContext context, TextRenderer textRenderer, CompassConfig config,
                                           int screenWidth, int screenHeight) {
        if (screenHeight < 300) return;

        float scale = (float) config.hudScale;
        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, 1.0f);
        try {
            int scaledWidth = Math.round(screenWidth / scale);
            int width = Math.min(config.hudWidth, Math.max(140, scaledWidth - 80));
            int centerX = scaledWidth / 2;
            int y = Math.round((screenHeight - 78) / scale);

            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Live preview"), centerX, y - 12, 0xA0A0A0);
            drawFrame(context, centerX - width / 2, y, width, config);
            drawHeading(context, textRenderer, centerX, centerX - width / 2, y, width, 0.0f, config);

            drawPreviewIcon(context, ICON_WAYPOINT, centerX - width * 3 / 10, y,
                    markerAlpha(MarkerType.NORMAL, 500.0, config));
            drawPreviewIcon(context, ICON_WAYSTONE, centerX - width / 10, y,
                    markerAlpha(MarkerType.WAYSTONE, 1500.0, config));
            drawPreviewIcon(context, ICON_DEATH, centerX + width / 10, y,
                    markerAlpha(MarkerType.DEATH, 5000.0, config));
            drawPreviewIcon(context, ICON_SPAWN, centerX + width * 3 / 10, y,
                    markerAlpha(MarkerType.SPAWN, 7500.0, config));
        } finally {
            context.getMatrices().pop();
        }
    }

    private static int angleToX(double relative, int centerX, int width, double halfSpan) {
        return centerX + (int) Math.round((relative / halfSpan) * (width / 2.0));
    }

    private static double wrapDegrees(double degrees) {
        return MathHelper.wrapDegrees(degrees);
    }

    private static Identifier id(String path) {
        return new Identifier(GooskersCompass.MOD_ID, path);
    }

    private record VisibleMarker(CompassMarker marker, int x, double relativeAngle, double distance) {}
}

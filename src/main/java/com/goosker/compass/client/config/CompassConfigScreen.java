package com.goosker.compass.client.config;

import com.goosker.compass.client.hud.CompassHudRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class CompassConfigScreen extends Screen {
    private final Screen parent;
    private final CompassConfig working;
    private int page;

    public CompassConfigScreen(Screen parent) {
        super(Text.literal("Goosker's Compass"));
        this.parent = parent;
        this.working = ConfigManager.get().copy();
    }

    @Override
    protected void init() {
        clearChildren();
        int columnWidth = Math.min(190, (width - 42) / 2);
        int left = width / 2 - columnWidth - 5;
        int right = width / 2 + 5;
        int y = 46;
        int row = 24;

        if (page == 0) {
            addToggle(left, y, columnWidth, "Enabled", () -> working.enabled, v -> working.enabled = v);
            addToggle(right, y, columnWidth, "Show heading ticks", () -> working.showTicks, v -> working.showTicks = v);
            y += row;
            addSlider(left, y, columnWidth, "HUD scale", 0.5, 2.0, working.hudScale, 2, v -> working.hudScale = v);
            addToggle(right, y, columnWidth, "Show N / E / S / W", () -> working.showCardinals, v -> working.showCardinals = v);
            y += row;
            addIntSlider(left, y, columnWidth, "Top offset", 0, 180, working.hudY, v -> working.hudY = v, " px");
            addToggle(right, y, columnWidth, "Show marker label", () -> working.showLabels, v -> working.showLabels = v);
            y += row;
            addIntSlider(left, y, columnWidth, "Compass width", 140, 600, working.hudWidth, v -> working.hudWidth = v, " px");
            addToggle(right, y, columnWidth, "Show distance", () -> working.showDistances, v -> working.showDistances = v);
            y += row;
            addIntSlider(left, y, columnWidth, "Visible heading span", 60, 180, working.compassSpanDegrees,
                    v -> working.compassSpanDegrees = v, "°");
            addToggle(right, y, columnWidth, "Hide with F3", () -> working.hideWithDebugHud, v -> working.hideWithDebugHud = v);
            y += row;
            addSlider(left, y, columnWidth, "Bar opacity", 0.15, 1.0, working.backgroundOpacity, 2,
                    v -> working.backgroundOpacity = v);
        } else {
            addToggle(left, y, columnWidth, "Xaero waypoints", () -> working.showNormalWaypoints, v -> working.showNormalWaypoints = v);
            addToggle(right, y, columnWidth, "Activated Waystones", () -> working.showWaystones, v -> working.showWaystones = v);
            y += row;
            addToggle(left, y, columnWidth, "Server waypoints", () -> working.showServerWaypoints, v -> working.showServerWaypoints = v);
            addToggle(right, y, columnWidth, "Bed / spawn", () -> working.showSpawn, v -> working.showSpawn = v);
            y += row;
            addToggle(left, y, columnWidth, "Death markers", () -> working.showDeaths, v -> working.showDeaths = v);
            addToggle(right, y, columnWidth, "Deaths ignore range", () -> working.deathsIgnoreRange, v -> working.deathsIgnoreRange = v);
            y += row;
            addIntSlider(left, y, columnWidth, "Death marker count", 0, 20, working.deathCount, v -> working.deathCount = v, "");
            addIntSlider(right, y, columnWidth, "Horizontal range", 50, 5000, working.horizontalRange,
                    v -> working.horizontalRange = v, " blocks");
            y += row;
            addIntSlider(right, y, columnWidth, "Vertical range", 10, 1000, working.verticalRange,
                    v -> working.verticalRange = v, " blocks");

            // Fade controls. These update the live preview immediately.
            addIntSlider(left, y, columnWidth, "Waypoint fade distance", 250, 5000, working.markerFadeDistance,
                    v -> working.markerFadeDistance = v, " blocks");
            y += row;
            addIntSlider(left, y, columnWidth, "Spawn/death fade distance", 1000, 20000, working.persistentFadeDistance,
                    v -> working.persistentFadeDistance = v, " blocks");
            addIntSlider(right, y, columnWidth, "Waypoint fade strength", 0, 100,
                    (int) Math.round(working.markerFadeStrength * 100.0),
                    v -> working.markerFadeStrength = v / 100.0, "%");
            y += row;
            addIntSlider(right, y, columnWidth, "Spawn/death fade strength", 0, 100,
                    (int) Math.round(working.persistentFadeStrength * 100.0),
                    v -> working.persistentFadeStrength = v / 100.0, "%");
        }

        int bottom = height - 28;
        addDrawableChild(ButtonWidget.builder(Text.literal(page == 0 ? "Markers >" : "< HUD"), b -> {
            page = page == 0 ? 1 : 0;
            clearAndInit();
        }).dimensions(width / 2 - 154, bottom, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> {
            working.sanitize();
            ConfigManager.set(working);
            ConfigManager.save();
            if (client != null) client.setScreen(parent);
        }).dimensions(width / 2 - 50, bottom, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> {
            if (client != null) client.setScreen(parent);
        }).dimensions(width / 2 + 54, bottom, 100, 20).build());
    }

    private void addToggle(int x, int y, int width, String label, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        addDrawableChild(CyclingButtonWidget.onOffBuilder(getter.get())
                .build(x, y, width, 20, Text.literal(label), (button, value) -> setter.accept(value)));
    }

    private void addIntSlider(int x, int y, int width, String label, int min, int max, int initial,
                              Consumer<Integer> setter, String suffix) {
        addDrawableChild(new ValueSlider(x, y, width, label, min, max, initial, 0,
                v -> setter.accept((int) Math.round(v)), suffix));
    }

    private void addSlider(int x, int y, int width, String label, double min, double max, double initial,
                           int decimals, Consumer<Double> setter) {
        addDrawableChild(new ValueSlider(x, y, width, label, min, max, initial, decimals, setter, ""));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 14, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(page == 0 ? "HUD" : "Markers"), width / 2, 29, 0xA0A0A0);

        // Live preview remains visible while sliders/toggles are being changed.
        CompassHudRenderer.renderConfigPreview(context, textRenderer, working, width, height);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    private static final class ValueSlider extends SliderWidget {
        private final String label;
        private final double min;
        private final double max;
        private final int decimals;
        private final Consumer<Double> setter;
        private final String suffix;

        private ValueSlider(int x, int y, int width, String label, double min, double max, double initial,
                            int decimals, Consumer<Double> setter, String suffix) {
            super(x, y, width, 20, Text.empty(), MathHelper.clamp((initial - min) / (max - min), 0.0, 1.0));
            this.label = label;
            this.min = min;
            this.max = max;
            this.decimals = decimals;
            this.setter = setter;
            this.suffix = suffix;
            updateMessage();
        }

        private double actual() {
            return min + value * (max - min);
        }

        @Override
        protected void updateMessage() {
            double actual = actual();
            String valueText = decimals == 0
                    ? Long.toString(Math.round(actual))
                    : String.format(java.util.Locale.ROOT, "%." + decimals + "f", actual);
            setMessage(Text.literal(label + ": " + valueText + suffix));
        }

        @Override
        protected void applyValue() {
            double actual = actual();
            if (decimals == 0) actual = Math.round(actual);
            setter.accept(actual);
        }
    }
}

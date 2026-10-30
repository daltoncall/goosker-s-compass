package com.goosker.compass.client.config;

public final class CompassConfig {
    public boolean enabled = true;

    public double hudScale = 1.0;
    public int hudY = 8;
    public int hudWidth = 300;
    public int compassSpanDegrees = 120;
    public double backgroundOpacity = 0.72;

    public int horizontalRange = 500;
    public int verticalRange = 100;
    public int deathCount = 3;

    public boolean showNormalWaypoints = true;
    public boolean showWaystones = true;
    public boolean showServerWaypoints = true;
    public boolean showSpawn = true;
    public boolean showDeaths = true;
    public boolean deathsIgnoreRange = true;
    public boolean showLabels = true;
    public boolean showDistances = true;
    public boolean showTicks = true;
    public boolean showCardinals = true;
    public boolean hideWithDebugHud = false;

    // Distance fading. Normal/server/Waystone markers use markerFadeDistance.
    // Spawn and death use the longer persistentFadeDistance.
    public int markerFadeDistance = 2500;
    public int persistentFadeDistance = 10000;
    public double markerFadeStrength = 1.0;
    public double persistentFadeStrength = 1.0;

    // Shift the complete vanilla boss-bar overlay below the compass.
    public boolean bossBarsBelowCompass = true;
    public int bossBarGap = 10;

    public void sanitize() {
        hudScale = clamp(hudScale, 0.5, 2.0);
        hudY = clamp(hudY, 0, 180);
        hudWidth = clamp(hudWidth, 140, 600);
        compassSpanDegrees = clamp(compassSpanDegrees, 60, 180);
        backgroundOpacity = clamp(backgroundOpacity, 0.15, 1.0);
        horizontalRange = clamp(horizontalRange, 50, 5000);
        verticalRange = clamp(verticalRange, 10, 1000);
        deathCount = clamp(deathCount, 0, 20);
        markerFadeDistance = clamp(markerFadeDistance, 250, 5000);
        persistentFadeDistance = clamp(persistentFadeDistance, 1000, 20000);
        markerFadeStrength = clamp(markerFadeStrength, 0.0, 1.0);
        bossBarGap = clamp(bossBarGap, 0, 80);
        persistentFadeStrength = clamp(persistentFadeStrength, 0.0, 1.0);

        // Normal markers must remain eligible for collection until they have fully faded.
        horizontalRange = Math.max(horizontalRange, markerFadeDistance);
    }

    public CompassConfig copy() {
        CompassConfig c = new CompassConfig();
        c.enabled = enabled;
        c.hudScale = hudScale;
        c.hudY = hudY;
        c.hudWidth = hudWidth;
        c.compassSpanDegrees = compassSpanDegrees;
        c.backgroundOpacity = backgroundOpacity;
        c.horizontalRange = horizontalRange;
        c.verticalRange = verticalRange;
        c.deathCount = deathCount;
        c.showNormalWaypoints = showNormalWaypoints;
        c.showWaystones = showWaystones;
        c.showServerWaypoints = showServerWaypoints;
        c.showSpawn = showSpawn;
        c.showDeaths = showDeaths;
        c.deathsIgnoreRange = deathsIgnoreRange;
        c.showLabels = showLabels;
        c.showDistances = showDistances;
        c.showTicks = showTicks;
        c.showCardinals = showCardinals;
        c.hideWithDebugHud = hideWithDebugHud;
        c.markerFadeDistance = markerFadeDistance;
        c.persistentFadeDistance = persistentFadeDistance;
        c.markerFadeStrength = markerFadeStrength;
        c.persistentFadeStrength = persistentFadeStrength;
        c.bossBarsBelowCompass = bossBarsBelowCompass;
        c.bossBarGap = bossBarGap;
        return c;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

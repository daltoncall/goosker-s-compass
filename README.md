# Goosker's Compass

A Fabric 1.20.1 horizontal compass HUD for Goosker's Medieval Modpack. It uses Minecraft's text renderer, custom pixel-art HUD assets, and Hytale/Skyrim-style bearing markers.

## Features

- N / E / S / W indicators with minor heading ticks
- No tick rendered directly behind the N / E / S / W letters
- Nearby Xaero waypoints
- Activated Waystones support
- XMXW-aware Waystone/Xaero de-duplication
- Server Waypoint support using the normal waypoint icon
- User-authored 20x20 marker assets for waypoint, Waystone, death, and spawn
- Three age-based death-marker stages: normal, decayed, fully decayed
- Death-stage thresholds scale automatically with the configured recent-death count
- Optional Forgotten Graves compatibility: a death marker disappears after the loaded grave is recovered
- Bed/custom respawn marker synced from the server; default world spawn is intentionally ignored
- Horizontal and vertical marker range filters
- Distance-based marker fading with separate waypoint and spawn/death distances and strengths
- Live compass preview in the Mod Menu config screen while adjusting settings
- Marker icons render above the N / E / S / W letters and heading ticks
- Custom 19x19 compass end-cap textures
- Full-width configurable translucent compass background
- Mod Menu configuration screen
- Configurable HUD width, position, scale, opacity, compass span, ranges, marker categories, labels, distances, and death count
- Reflection/optional-integration design: Xaero, Waystones, XMXW, Server Waypoint, and Forgotten Graves are not bundled

## Defaults

- Waypoint fade-out distance: 2,500 blocks (90% at 250, 80% at 500 with default strength)
- Spawn/death fade-out distance: 10,000 blocks
- Waypoint range: automatically at least the waypoint fade-out distance
- Vertical range: 100 blocks
- Death markers: 3 most recent
- Deaths ignore normal distance limits: enabled
- Compass span: 120 degrees

## Build

The GitHub Actions build validates the boss-bar mixin metadata before publishing the artifact.

Requires Java 17.

Windows:

```bat
gradlew.bat build
```

Linux/macOS:

```sh
./gradlew build
```

The included bootstrap wrapper downloads Gradle 8.7 on first use if the standard wrapper JAR is not present. Output jars are written to `build/libs/`.

## Runtime

Required:

- Fabric Loader
- Fabric API

Optional integrations:

- Mod Menu 7.x
- Xaero's Minimap 25.x
- Xaero's World Map 1.40.x
- Waystones 14.x
- XMXW 2.x
- Server Waypoint 3.x
- Forgotten Graves 3.x

The bed/spawn indicator requires Goosker's Compass on the server as well as the client. It only appears after the player explicitly sets a respawn point; vanilla's default world spawn is intentionally ignored. The compass itself and client-side waypoint integrations still work without server installation.

Forgotten Graves compatibility is client-side and optional. If a death/grave position is in a loaded chunk and the Forgotten Graves grave block entity is gone, the compass death marker is hidden. Unloaded grave chunks keep their marker until the client can verify the grave state.

## Config

`config/gooskers-compass.json`

Or use **Mods -> Goosker's Compass -> Configure** with Mod Menu installed.

### Boss-bar layout (beta-1.1.14)

Under **Mod Menu → Goosker's Compass → Layout**, enable **Boss bars below compass** (on by default) and choose the gap in pixels. The layout page previews a sample Ender Dragon bar below the compass. This moves the entire vanilla boss-bar overlay, including its title, and does not alter boss mechanics.

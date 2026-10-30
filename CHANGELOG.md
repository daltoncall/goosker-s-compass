## beta-1.1.14
- Optional vertical boss-bar relocation below the compass (includes boss-bar title).
- New Layout page in Mod Menu: adjustable boss-bar gap (0–80 px), with live compass and boss-bar preview.
- Preserve default behavior with the toggle disabled. No marker or texture changes.

# Changelog

## beta-1.1.13

- Added distance-based opacity fading for all compass marker icons.
- Waypoint, Waystone, and server-waypoint markers default to 90% opacity at 250 blocks, 80% at 500 blocks, and fully fade at 2,500 blocks.
- Spawn and death markers use a longer 10,000-block fade-out distance.
- Added configurable fade-distance and fade-strength controls for both marker groups.
- Added a live compass preview to the config screen so HUD changes can be viewed without closing Mod Menu.
- Spawn markers are allowed beyond the normal waypoint range so the longer persistent fade can work.

## beta-1.1.12

- Marker icons now render above N / E / S / W and heading ticks, so letters cleanly disappear behind overlapping markers.

## 0.1.11

- Replaced both compass end-cap textures with the final user-authored 19x19 cap artwork.
- Preserved the full-width translucent compass background and current frame geometry.

## 0.1.10

- Added custom 19x19 left and right compass end caps.
- Fixed the compass background fill so it spans the entire configured HUD width.
- Preserved the downward-pointing center marker, cardinal tick exclusions, death stages, and Forgotten Graves compatibility.

## 0.1.9

- Reworked Forgotten Graves compatibility to avoid a hard class dependency.
- Death markers remain visible while their chunk is unloaded and disappear when the loaded grave position no longer contains a Forgotten Graves grave block entity.

## 0.1.8

- Fixed the Fabric Loader invocation used by Forgotten Graves compatibility.

## 0.1.7

- Added optional Forgotten Graves support for hiding collected grave/death markers.

## 0.1.6

- Added three death-marker age stages.
- Newest third uses the normal death icon.
- Middle third uses the decayed death icon.
- Oldest third uses the fully decayed death icon.
- The age thresholds scale automatically with the configured death-marker count.

## 0.1.5

- Corrected user-authored marker mapping: waypoint flag, Waystone pillar, death icon, and spawn bed.
- Removed heading ticks directly behind N / E / S / W.
- Changed the center indicator to a downward-pointing triangle with no lower tail pixels.

## 0.1.4-alpha

- Rebuilt all four compass markers directly on native 20x20 canvases.
- No source icon is downsampled or rescaled.
- Waypoint, Waystone, Death, and Spawn each use a true 20x20 PNG.
- Spawn icon behavior remains unchanged: no marker until the player explicitly sets a respawn point.

## 0.1.3-alpha

- Rebuilt all four compass marker textures as true native 20x20 pixel sprites.
- Removed resampling from the large generated concept art.
- Spawn marker behavior is unchanged: no bed icon appears until the player explicitly sets a respawn point.

## 0.1.2-alpha

- Reverted the compass marker set to the first approved grayscale icons.
- Waypoint uses the original flag/emblem icon.
- Waystone uses the original pillar/monument icon.
- Death uses the original gravestone icon.
- Spawn uses the original bed icon.

## 0.1.1-alpha

- Replaced the original marker textures with the approved four-icon grayscale pixel set.
- Waypoint and server-waypoint markers share the flag/emblem icon.
- Waystones use the rune pillar icon.
- Death markers use the gravestone icon.
- Custom spawn uses the bed icon.
- Increased marker render size to 20 px.
- Custom-spawn marker explicitly requires a player-set respawn position.

## 0.1.0-alpha

- Initial compass HUD prototype.
- Xaero, Waystones, XMXW and Server Waypoint integration.
- Recent-death filtering, custom spawn sync and Mod Menu config screen.

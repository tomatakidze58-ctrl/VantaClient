# Vanta Client 26.2

Vanta Client is an original purple/black Fabric client project for Minecraft 26.2.
It is designed as a legitimate HUD, PvP-information and performance client rather
than a cheat client.

## Controls
- **Right Shift** — open the Vanta module menu
- **C (hold)** — Zoom (when Zoom is enabled)

## Working feature set in this source
### HUD
- FPS Counter
- Ping Display
- Coordinates
- Direction
- Armor HUD
- Hunger + Saturation
- Totem Counter
- Potion Effect count
- Keystrokes
- CPS Counter
- Memory Usage
- Clock
- Server Address
- Player Count

### PvP / visual
- Auto Sprint
- Vanta Crosshair
- Hold-to-Zoom with FOV restoration
- Fullbright lightmap override + gamma fallback
- View Bobbing Off
- Entity Shadows Off
- Minimal Particles

### Performance
- **FPS Boost preset** — reversible vanilla-option preset
  - render-distance cap
  - simulation-distance cap
  - lower entity-distance scaling where the option is available
  - minimal particles
  - entity shadows off
  - view bobbing off
- Dynamic FPS while unfocused, with the original FPS limit restored afterward
- Standalone Render Distance Cap
- Standalone Entity Distance reduction
- Standalone Chunk Performance cap
- Standalone Fast Particles

The performance preset does not claim a fixed FPS increase. Its effect depends on
your hardware, world, shaders/resource packs and server situation.

## 26.2 implementation notes
Minecraft 26.2 uses Java 25 and Mojang/unobfuscated game names. Vanta uses the
26.2 Fabric GUI/HUD APIs (`GuiGraphicsExtractor`, `HudElementRegistry`) and avoids
raw OpenGL so it remains compatible with Minecraft's rendering abstraction.

## Build on GitHub
The repository includes `.github/workflows/main.yml`.

1. Upload the **contents** of this project to your GitHub repository.
2. Open **Actions → Build Vanta Client**.
3. Wait for a green build.
4. Open the successful run and download the **VantaClient** artifact.
5. Extract it and use the normal `VantaClient-*.jar` (not the `-sources.jar`).
6. Put it in the Fabric 26.2 `mods` folder together with Fabric API.

## Important
This project contains Vanta's own source code. It does not bundle or copy the
third-party mod JARs that were used as feature inspiration.

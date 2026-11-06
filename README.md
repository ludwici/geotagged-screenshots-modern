# Geotagged Screenshots Modern — NeoForge 26.2

A client-side mod for Minecraft **26.2** that creates screenshot waypoints and displays screenshot previews inside Xaero's World Map.

This branch targets **NeoForge** and Minecraft **26.2** only. Earlier Minecraft releases have separate branches. This branch is separate from the Minecraft 1.21.1 branches.

## Requirements

- Minecraft 26.2
- Java 25
- 26.2.0.93
- Xaero's World Map 1.47.2
- Xaero's Minimap 26.6.2

## Features

- Save original screenshots as PNG or JPEG (92% JPEG quality).
- Temporarily hide the HUD during screenshots and restore its previous state.
- Create Xaero waypoint markers with screenshot thumbnails.
- View a screenshot at full size by selecting its marker in Xaero's World Map.
- Recreate missing thumbnails from original screenshots.
- Optional screenshot success messages and separate screenshot waypoint set.

## Configuration

The client settings are stored in `config/geotagged_screenshots-client.toml`.

## Building

Use JDK 25 and Gradle 9.5.1:

```shell
gradle clean build
```

The `build` task runs the unit tests and creates the NeoForge JAR in `build/libs`. GitHub Actions builds and uploads it on pushes to `26.2-neo`.

**Testing:** CI checks compilation and JPEG encoding. In-game behavior, map marker interactions, HUD restoration, and runtime mixin compatibility still require testing on an actual client with both Xaero mods.

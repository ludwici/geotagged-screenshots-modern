# Geotagged Screenshots Modern — Fabric 26.1.2

A client-side mod for Minecraft **26.1.2** that creates screenshot waypoints and displays screenshot previews inside Xaero's World Map.

This branch targets **Fabric** and Minecraft **26.1.2**. The 26.1 and 26.1.1 clients are not declared compatible: the selected Xaero and loader dependencies have only been built and checked against 26.1.2. This branch is separate from the Minecraft 1.21.1 branches.

## Requirements

- Minecraft 26.1.2
- Java 25
- Fabric Loader 0.19.5 and Fabric API 0.155.3+26.1.2
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

The client settings are stored in `config/geotagged_screenshots-client.json`.

## Building

Use JDK 25 and Gradle 9.7.0:

```shell
gradle clean build
```

The `build` task runs the unit tests and creates the Fabric JAR in `build/libs`. GitHub Actions builds and uploads it on pushes to `26.1.2-fabric`.

**Testing:** CI checks compilation and JPEG encoding. In-game behavior, map marker interactions, HUD restoration, and runtime mixin compatibility still require testing on an actual client with both Xaero mods.

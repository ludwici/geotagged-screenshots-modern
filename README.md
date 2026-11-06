# Geotagged Screenshots Modern — Fabric 1.21.1

A client-side Minecraft mod that places screenshot waypoints in **Xaero's Minimap** and shows image previews in **Xaero's World Map**.

This branch targets **Minecraft 1.21.1 with Fabric** exclusively. The NeoForge port is maintained separately in the `1.21.1-neo` branch.

## Dependencies

- Fabric Loader 0.19.3
- Fabric API 0.116.13+1.21.1
- Xaero's World Map 1.44.2 for Fabric
- Xaero's Minimap 26.4.2 for Fabric
- Java 21

The versions are aligned with the Fabric branch of [CobblemonRaidDenIcons](https://github.com/ludwici/CobblemonRaidDenIcons/tree/fabric).

## Features

- Capture PNG or real JPEG screenshots (JPEG quality 92%).
- Automatically hide HUD while capturing and restore the F1 state.
- Create image thumbnails and screenshot waypoints.
- Open screenshots from the Xaero World Map waypoint marker.
- Recreate missing thumbnails from their original screenshots.
- Disable screenshot success chat messages.

## Configuration

The client configuration file is automatically generated at `config/geotagged_screenshots-client.json` on launch. Restart the client after editing it.

## Build

Use Gradle 9.5.1 and JDK 21:

```bash
gradle clean build
```

The GitHub Actions workflow builds the Fabric JAR and runs unit tests on pushes to `1.21.1-fabric`.

## Status

Successful compilation does not replace in-game verification. Test screenshot capture, waypoint creation, preview clicks, JPEG reopening, and F1 state restoration in a Fabric 1.21.1 client.

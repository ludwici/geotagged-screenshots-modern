# Geotagged Screenshots Modern

A maintained **client-side** fork of [Geotagged Screenshots](https://github.com/CCr4ft3r/geotagged-screenshots), originally created by **CCr4ft3r** and maintained/ported by **[ludwici](https://github.com/ludwici)**.

Screenshots become location-linked waypoints in Xaero's World Map and Minimap, so you can return to memorable places and view captured images from the map.

**Branch:** `1.21.1-fabric` · **Minecraft:** `1.21.1` · **Platform:** Fabric · **Environment:** Client only

## Features

- Save Minecraft screenshots as PNG or JPEG (configurable).
- Hide the HUD for a screenshot and restore its previous F1 state.
- Create Xaero waypoint markers for screenshots and show thumbnails on Xaero's World Map.
- Click screenshot markers on the world map to view the full-size image.
- Restore missing thumbnails from original screenshot files.

## Requirements

- Minecraft `1.21.1`
- Fabric Loader `0.19.3`
- Fabric API `0.116.13+1.21.1`
- Xaero's World Map `1.44.2`
- Xaero's Minimap `26.4.2`
- Java 21

Install this mod and its dependencies on the **client**. A dedicated server does not need Geotagged Screenshots installed. Singleplayer and multiplayer clients are supported.

## Configuration

The client settings are stored in `config/geotagged_screenshots-client.json`.

Options include JPEG/PNG output, hiding the HUD while taking screenshots, screenshot waypoint sets, and screenshot/thumbnail chat messages.

## FAQ

**I can't see screenshot previews on the world map. What should I check?**

Make sure the screenshot waypoints are enabled and the `Geotagged Screenshots` waypoint set is selected in Xaero. Existing screenshots also need their corresponding waypoints.

**Can I include this mod in a modpack?**

Yes, provided you follow the LGPL-3.0 license terms and preserve applicable attribution and license notices.

**Where can I report a bug or request a feature?**

Use the [issue tracker](https://github.com/ludwici/geotagged-screenshots-modern/issues).

## Building

Use JDK 21 and run:

```shell
gradle clean build
```

GitHub Actions runs the build and uploads compiled JAR files. A successful build does not replace testing in Minecraft with Xaero's mods installed.

## Credits and license

- **Original project and author:** [Geotagged Screenshots — CCr4ft3r](https://github.com/CCr4ft3r/geotagged-screenshots)
- **Maintained fork and ports:** [ludwici](https://github.com/ludwici/geotagged-screenshots-modern)
- **License:** [GNU LGPL-3.0](LICENSE)

This fork updates the mod for newer Minecraft versions and includes changes to screenshot storage, previews, waypoint integration and client-side compatibility. The original author's credits and copyright notices remain applicable.

## Links

- [Source code](https://github.com/ludwici/geotagged-screenshots-modern)
- [Issues and feature requests](https://github.com/ludwici/geotagged-screenshots-modern/issues)
- [Original mod](https://github.com/CCr4ft3r/geotagged-screenshots)

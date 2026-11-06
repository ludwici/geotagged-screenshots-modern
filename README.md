# Geotagged Screenshots Modern

A maintained **client-side** fork of [Geotagged Screenshots](https://github.com/CCr4ft3r/geotagged-screenshots), originally created by **CCr4ft3r** and maintained/ported by **[ludwici](https://github.com/ludwici)**.

Screenshots become location-linked waypoints in Xaero's World Map and Minimap, so you can revisit places and view saved images from the map.

**Branch:** `26.1.2-neo` · **Minecraft:** `26.1.2` · **Platform:** NeoForge · **Environment:** Client only

## Features

- Save screenshots as PNG or JPEG (configurable).
- Hide the HUD for a screenshot and restore the previous F1 state.
- Create Xaero screenshot waypoints with thumbnails on World Map.
- Open full-size screenshots from their markers in Xaero's World Map.
- Recreate missing thumbnails from original screenshot files.
- Configure waypoint sets and screenshot-related chat messages.

## Requirements

- Minecraft `26.1.2`
- NeoForge `26.1.2.124`
- Xaero's World Map `1.47.2`
- Xaero's Minimap `26.6.2`
- Java 25

Install this mod and its dependencies on the **client**. Dedicated servers do not need Geotagged Screenshots installed. This branch targets Minecraft 26.1.2; other versions have separate branches.

## Configuration

The client settings are stored in `config/geotagged_screenshots-client.toml`.

## FAQ

**I cannot see screenshots on Xaero's World Map.**

Check that screenshot waypoints are enabled and that the `Geotagged Screenshots` waypoint set is selected. An image needs its matching waypoint to appear on the map.

**Can I include this mod in a modpack?**

Yes, subject to the LGPL-3.0 license and applicable copyright and attribution notices.

**How do I report a bug or request a feature?**

Open an issue in the [maintained fork's issue tracker](https://github.com/ludwici/geotagged-screenshots-modern/issues).

## Building

Use JDK 25 and run:

```shell
gradle clean build
```

GitHub Actions builds and tests the project and uploads the JAR artifact. In-game compatibility and Xaero interactions should also be verified on a client.

## Credits and license

- **Original author:** [CCr4ft3r — Geotagged Screenshots](https://github.com/CCr4ft3r/geotagged-screenshots)
- **Maintainer of this fork:** [ludwici](https://github.com/ludwici/geotagged-screenshots-modern)
- **License:** [GNU LGPL-3.0](LICENSE)

This maintained fork updates the mod for newer Minecraft versions and adapts screenshot saving, previews, waypoints and client integrations. Original copyright notices and license obligations remain in effect.

## Links

- [Source code](https://github.com/ludwici/geotagged-screenshots-modern)
- [Issues and feature requests](https://github.com/ludwici/geotagged-screenshots-modern/issues)
- [Original project](https://github.com/CCr4ft3r/geotagged-screenshots)

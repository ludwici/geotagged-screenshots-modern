# Geotagged Screenshots Modern

A maintained **client-side** fork of [Geotagged Screenshots](https://github.com/CCr4ft3r/geotagged-screenshots), originally created by **CCr4ft3r** and maintained/ported by **[ludwici](https://github.com/ludwici)**.

Screenshots become location-linked Xaero waypoints, making it easier to find places and revisit saved images in Xaero's World Map.

**Branch:** `26.2-neo` · **Minecraft:** `26.2` · **Platform:** NeoForge · **Environment:** Client only

## Features

- Save screenshots as PNG or JPEG (configurable).
- Hide the HUD for a screenshot and restore the previous F1 state.
- Create screenshot waypoints with thumbnail previews on Xaero's World Map.
- Open full-size screenshots from waypoint markers.
- Recreate missing thumbnails from original screenshot files.
- Configure waypoint sets and screenshot-related chat messages.

## Requirements

- Minecraft `26.2`
- NeoForge `26.2.0.93`
- Xaero's World Map `1.47.2`
- Xaero's Minimap `26.6.2`
- Java 25

Install this mod and its dependencies on the **client**. A dedicated server does not need Geotagged Screenshots. Other Minecraft versions are maintained in separate branches.

## Configuration

The client settings are stored in `config/geotagged_screenshots-client.toml`.

## FAQ

**I cannot see screenshots on Xaero's World Map.**

Check that screenshot waypoints are enabled and that the `Geotagged Screenshots` waypoint set is selected. Images need corresponding waypoints.

**Can I include this mod in a modpack?**

Yes, subject to the LGPL-3.0 license and applicable copyright notices.

**Where can I report bugs or suggest features?**

Use the [maintained fork's issue tracker](https://github.com/ludwici/geotagged-screenshots-modern/issues).

## Building

Use JDK 25 and run `gradle clean build`. GitHub Actions builds, tests and uploads the JAR. Test gameplay and Xaero integrations in Minecraft as well.

## Credits and license

- **Original author:** [CCr4ft3r — Geotagged Screenshots](https://github.com/CCr4ft3r/geotagged-screenshots)
- **Fork maintainer:** [ludwici](https://github.com/ludwici/geotagged-screenshots-modern)
- **License:** [GNU LGPL-3.0](LICENSE)

This maintained fork contains updates for modern Minecraft releases and changes to screenshot saving, waypoint previews and client compatibility. Original copyright notices and license obligations continue to apply.

## Links

- [Source code](https://github.com/ludwici/geotagged-screenshots-modern)
- [Issues and feature requests](https://github.com/ludwici/geotagged-screenshots-modern/issues)
- [Original project](https://github.com/CCr4ft3r/geotagged-screenshots)

package com.ccr4ft3r.geotaggedscreenshots;

import com.ccr4ft3r.geotaggedscreenshots.config.ClientConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;

public class ModConstants {
    public static final String MODID = "geotagged_screenshots_modern";
    public static final File SCREENSHOTS_DIR = FabricLoader.getInstance().getGameDir().resolve("screenshots").toFile();
    public static final File THUMBNAIL_DIR = new File(SCREENSHOTS_DIR, "geotagged_thumbnails");

    public static String getWaypointSet() {
        return ClientConfig.CONFIG_DATA.useSeparateSetForGeotaggedScreenshots
                ? ClientConfig.CONFIG_DATA.waypointSetName
                : "gui.xaero_default";
    }
}

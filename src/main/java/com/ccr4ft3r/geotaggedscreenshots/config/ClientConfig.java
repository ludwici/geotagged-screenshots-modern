package com.ccr4ft3r.geotaggedscreenshots.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final Data CONFIG_DATA = new Data(BUILDER);
    public static final ModConfigSpec CONFIG = BUILDER.build();

    public static class Data {
        public final ModConfigSpec.BooleanValue useSeparateSetForGeotaggedScreenshots;
        public final ModConfigSpec.ConfigValue<String> waypointSetName;
        public final ModConfigSpec.BooleanValue displayThumbnailRegenerationInChat;
        public final ModConfigSpec.BooleanValue hideUiAtTakingScreenshots;
        public final ModConfigSpec.BooleanValue disableScreenshotChatMessage;
        public final ModConfigSpec.BooleanValue useJpgForScreenshots;

        public Data(ModConfigSpec.Builder builder) {
            useSeparateSetForGeotaggedScreenshots = builder
                    .comment("Store screenshot waypoints in a separate Xaero waypoint set.")
                    .define("useSeparateSetForGeotaggedScreenshots", true);
            waypointSetName = builder
                    .comment("Waypoint set name for screenshots.")
                    .define("waypointSetName", "Geotagged Screenshots");
            displayThumbnailRegenerationInChat = builder
                    .comment("Display thumbnail regeneration progress.")
                    .define("displayThumbnailRegenerationInChat", true);
            hideUiAtTakingScreenshots = builder
                    .comment("Hide the interface when taking screenshots. Reserved for compatibility.")
                    .define("hideUiAtTakingScreenshots", false);
            disableScreenshotChatMessage = builder
                    .comment("Suppress the screenshot success message.")
                    .define("disableScreenshotChatMessage", true);
            useJpgForScreenshots = builder
                    .comment("Legacy setting. Screenshots are saved as PNG in NeoForge 1.21.1.")
                    .define("useJpgForScreenshots", false);
        }
    }
}

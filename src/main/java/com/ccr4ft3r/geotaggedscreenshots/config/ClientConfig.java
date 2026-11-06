package com.ccr4ft3r.geotaggedscreenshots.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ccr4ft3r.geotaggedscreenshots.ModConstants;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Data CONFIG_DATA = new Data();

    private ClientConfig() {
    }

    public static void load() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path file = configDir.resolve(ModConstants.MODID + "-client.json");
        Path legacy = configDir.resolve("geotagged_screenshots-client.json");

        try {
            Files.createDirectories(file.getParent());
            if (Files.isRegularFile(legacy) && Files.notExists(file)) {
                Files.copy(legacy, file);
            }

            if (Files.isRegularFile(file)) {
                Data saved = GSON.fromJson(Files.readString(file), Data.class);
                if (saved != null) {
                    CONFIG_DATA = saved;
                }
            } else {
                Files.writeString(file, GSON.toJson(CONFIG_DATA));
            }

            if (CONFIG_DATA.waypointSetName == null || CONFIG_DATA.waypointSetName.isBlank()) {
                CONFIG_DATA.waypointSetName = "Geotagged Screenshots";
            }
        } catch (IOException | RuntimeException e) {
            LogUtils.getLogger().error("Couldn't load Geotagged Screenshots client configuration", e);
            CONFIG_DATA = new Data();
        }
    }

    public static final class Data {
        public boolean useSeparateSetForGeotaggedScreenshots = true;
        public String waypointSetName = "Geotagged Screenshots";
        public boolean displayThumbnailRegenerationInChat = true;
        public boolean hideUiAtTakingScreenshots = true;
        public boolean disableScreenshotChatMessage = true;
        public boolean useJpgForScreenshots = true;
    }
}

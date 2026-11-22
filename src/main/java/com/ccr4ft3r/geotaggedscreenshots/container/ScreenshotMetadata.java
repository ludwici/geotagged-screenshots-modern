package com.ccr4ft3r.geotaggedscreenshots.container;

import com.ccr4ft3r.geotaggedscreenshots.ModConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ScreenshotMetadata {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();
    private static String worldId;

    private ScreenshotMetadata() {
    }

    public static String currentWorldId() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }

        if (minecraft.getSingleplayerServer() != null) {
            return "singleplayer:" + minecraft.getSingleplayerServer().getWorldData().getLevelName();
        }

        if (minecraft.getCurrentServer() != null) {
            return "multiplayer:" + minecraft.getCurrentServer().ip;
        }

        return "world:" + minecraft.level.dimension().location();
    }

    public static void load(String id) {
        if (id.equals(worldId)) {
            return;
        }

        worldId = id;
        ENTRIES.clear();
        Path path = path();
        if (!Files.isRegularFile(path)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Entry[] entries = GSON.fromJson(reader, Entry[].class);
            if (entries != null) {
                for (Entry entry : entries) {
                    if (entry != null && entry.name() != null && entry.dimension() != null) {
                        ENTRIES.put(entry.name(), entry);
                    }
                }
            }
        } catch (Exception exception) {
            LogUtils.getLogger().warn("Could not load screenshot locations from {}", path, exception);
        }
    }

    public static Collection<Entry> entries() {
        return java.util.List.copyOf(ENTRIES.values());
    }

    public static void saveScreenshot(String name, Vec3 position, ResourceKey<Level> dimension) {
        put(name, position, dimension);
    }

    public static void importWaypoint(String name, Vec3 position, ResourceKey<Level> dimension) {
        if (!ENTRIES.containsKey(name)) {
            put(name, position, dimension);
        }
    }

    private static void put(String name, Vec3 position, ResourceKey<Level> dimension) {
        if (worldId == null) {
            return;
        }

        ENTRIES.put(name, new Entry(name, (int) Math.floor(position.x), (int) Math.floor(position.y),
                (int) Math.floor(position.z), dimension.location().toString()));
        write();
    }

    private static Path path() {
        String id = UUID.nameUUIDFromBytes(worldId.getBytes(StandardCharsets.UTF_8)).toString();
        return ModConstants.SCREENSHOTS_DIR.toPath().resolve("geotagged_locations").resolve(id + ".json");
    }

    private static void write() {
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(ENTRIES.values(), writer);
            }
        } catch (Exception exception) {
            LogUtils.getLogger().error("Could not save screenshot locations to {}", path, exception);
        }
    }

    public record Entry(String name, int x, int y, int z, String dimension) {
    }
}

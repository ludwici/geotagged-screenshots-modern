package com.ccr4ft3r.geotaggedscreenshots;

import com.ccr4ft3r.geotaggedscreenshots.config.ClientConfig;
import com.ccr4ft3r.geotaggedscreenshots.container.GeotaggedScreenshot;
import com.ccr4ft3r.geotaggedscreenshots.container.ImageType;
import com.ccr4ft3r.geotaggedscreenshots.container.WorldScreenshotAlbum;
import com.ccr4ft3r.geotaggedscreenshots.util.ImageUtil;
import com.ccr4ft3r.geotaggedscreenshots.util.xaero.XaeroWaypointUtil;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.map.gui.GuiMap;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class GeotaggedScreenshots implements ClientModInitializer {
    public static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    public static WorldScreenshotAlbum ALBUM;

    @Override
    public void onInitializeClient() {
        ClientConfig.load();
        ClientTickEvents.END_CLIENT_TICK.register(GeotaggedScreenshots::onClientTick);
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof GuiMap) {
                client.execute(GeotaggedScreenshots::load);
            }
        });
    }

    private static void onClientTick(Minecraft client) {
        if (client.level == null) {
            closeAlbum();
            return;
        }

        MinimapWorld world = getCurrentWaypointWorld();
        if (world == null) {
            return;
        }

        String worldId = world.toString();
        if (ALBUM != null && !ALBUM.getId().equals(worldId)) {
            closeAlbum();
        }

        WaypointSet set = world.getWaypointSet(ModConstants.getWaypointSet());
        if (set != null && (ALBUM == null || ALBUM.getScreenshots().size() < set.getWaypoints().size())) {
            load();
        }
    }

    private static MinimapWorld getCurrentWaypointWorld() {
        MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
        return session == null ? null : session.getWorldManager().getCurrentWorld();
    }

    private static void closeAlbum() {
        if (ALBUM != null) {
            ALBUM.getScreenshots().forEach(GeotaggedScreenshot::close);
            ALBUM = null;
        }
    }

    public static void load() {
        if (Minecraft.getInstance().level == null) {
            return;
        }

        MinimapWorld world = getCurrentWaypointWorld();
        if (world == null) {
            return;
        }

        String newWorldId = world.toString();
        WaypointSet screenshotSet = XaeroWaypointUtil.getScreenshotWaypointSet();
        if (screenshotSet == null) {
            return;
        }

        if (ALBUM == null || !ALBUM.getId().equals(newWorldId)) {
            closeAlbum();
            ALBUM = new WorldScreenshotAlbum(newWorldId);
        }

        int discovered = 0;
        for (var waypoint : screenshotSet.getWaypoints()) {
            if (ALBUM.getScreenshot(waypoint.getName()) == null) {
                GeotaggedScreenshot screenshot = new GeotaggedScreenshot(waypoint.getName());
                ALBUM.add(screenshot);
                screenshot.getImageFuture(ImageType.THUMBNAIL);
                discovered++;
            }
        }

        if (discovered > 0) {
            regenerateMissingThumbnails();
        }

        XaeroWaypointUtil.requestWorldMapRefresh();
        LogUtils.getLogger().info("Geotagged album synchronized: {} screenshot(s), {} newly discovered", ALBUM.getScreenshots().size(), discovered);
    }

    private static void regenerateMissingThumbnails() {
        List<GeotaggedScreenshot> missing = ALBUM.getScreenshots().stream()
                .filter(screenshot -> !screenshot.getFile(ImageType.THUMBNAIL).exists()
                        && screenshot.getFile(ImageType.ORIGINAL).exists())
                .toList();

        if (missing.isEmpty()) {
            return;
        }

        AtomicInteger completed = new AtomicInteger();
        LogUtils.getLogger().info("Regenerating thumbnails for {} screenshots", missing.size());
        displayMessage("started_thumbnail_regeneration", missing.size());

        for (GeotaggedScreenshot screenshot : missing) {
            EXECUTOR.submit(() -> {
                File originalFile = screenshot.getFile(ImageType.ORIGINAL);
                File thumbnailFile = null;
                try {
                    NativeImage image = ImageUtil.loadImage(originalFile).join();
                    if (image != null) {
                        try (image) {
                            thumbnailFile = ImageUtil.createThumbnail(image, originalFile);
                        }
                    }
                } catch (Exception e) {
                    LogUtils.getLogger().error("Couldn't regenerate thumbnail for {}", originalFile, e);
                }

                File generatedThumbnail = thumbnailFile;
                Minecraft.getInstance().execute(() -> {
                    if (generatedThumbnail != null && generatedThumbnail.isFile()) {
                        screenshot.setFile(ImageType.THUMBNAIL, generatedThumbnail);
                        screenshot.getImageFuture(ImageType.THUMBNAIL);
                    }

                    if (completed.incrementAndGet() == missing.size()) {
                        displayMessage("completed_thumbnail_regeneration", missing.size());
                    }
                });
            });
        }
    }

    private static void displayMessage(String key, int count) {
        if (!ClientConfig.CONFIG_DATA.displayThumbnailRegenerationInChat) {
            return;
        }

        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().gui.getChat().addClientSystemMessage(Component.translatable("message." + ModConstants.MODID + "." + key, count));
            }
        });
    }
}

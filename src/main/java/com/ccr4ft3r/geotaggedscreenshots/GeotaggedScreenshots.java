package com.ccr4ft3r.geotaggedscreenshots;

import com.ccr4ft3r.geotaggedscreenshots.compat.MapCompat;
import com.ccr4ft3r.geotaggedscreenshots.config.ClientConfig;
import com.ccr4ft3r.geotaggedscreenshots.container.GeotaggedScreenshot;
import com.ccr4ft3r.geotaggedscreenshots.container.ImageType;
import com.ccr4ft3r.geotaggedscreenshots.container.ScreenshotMetadata;
import com.ccr4ft3r.geotaggedscreenshots.container.WorldScreenshotAlbum;
import com.ccr4ft3r.geotaggedscreenshots.util.ImageUtil;
import com.ccr4ft3r.geotaggedscreenshots.util.xaero.XaeroWaypointUtil;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class GeotaggedScreenshots implements ClientModInitializer {
    public static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    public static WorldScreenshotAlbum ALBUM;
    private static Runnable journeyRefresh;
    private static boolean xaeroSynchronized;
    private static int xaeroRetryTicks;
    private static final java.util.Set<String> nonScreenshotWaypoints = new java.util.HashSet<>();

    @Override
    public void onInitializeClient() {
        ClientConfig.load();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(GeotaggedScreenshots::load));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> closeAlbum());
        ClientTickEvents.END_CLIENT_TICK.register(GeotaggedScreenshots::retryXaeroSync);
    }

    private static void retryXaeroSync(Minecraft client) {
        if (client.level == null) {
            return;
        }

        if (ALBUM == null) {
            load();
        }

        if (ALBUM == null || xaeroSynchronized || !MapCompat.hasXaero()) {
            return;
        }

        if (++xaeroRetryTicks < 20) {
            return;
        }

        xaeroRetryTicks = 0;
        xaeroSynchronized = XaeroWaypointUtil.loadExistingWaypoints();
        if (xaeroSynchronized) {
            regenerateMissingThumbnails();
            if (journeyRefresh != null) {
                journeyRefresh.run();
            }
        }
    }

    private static void closeAlbum() {
        if (ALBUM != null) {
            ALBUM.getScreenshots().forEach(GeotaggedScreenshot::close);
            ALBUM = null;
        }

        nonScreenshotWaypoints.clear();
        xaeroSynchronized = false;
        xaeroRetryTicks = 0;
    }

    public static GeotaggedScreenshot findXaeroScreenshot(String name, int x, int y, int z) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !MapCompat.hasXaero()) {
            return null;
        }

        if (ALBUM == null) {
            String worldId = ScreenshotMetadata.currentWorldId();
            if (worldId == null) {
                return null;
            }

            if (loadAlbum(worldId)) {
                regenerateMissingThumbnails();
            }
        }

        GeotaggedScreenshot screenshot = ALBUM.getScreenshot(name);
        if (screenshot != null) {
            return screenshot;
        }

        if (nonScreenshotWaypoints.contains(name)) {
            return null;
        }

        screenshot = new GeotaggedScreenshot(name);
        if (!screenshot.getFile(ImageType.ORIGINAL).isFile()) {
            nonScreenshotWaypoints.add(name);
            return null;
        }

        ALBUM.add(screenshot);
        ScreenshotMetadata.importWaypoint(name, new Vec3(x, y, z), minecraft.level.dimension());
        regenerateMissingThumbnails();
        if (journeyRefresh != null) {
            journeyRefresh.run();
        }

        return screenshot;
    }

    public static void setJourneyRefresh(Runnable callback) {
        journeyRefresh = callback;
    }

    private static boolean loadAlbum(String worldId) {
        if (ALBUM != null && ALBUM.getId().equals(worldId)) {
            return false;
        }

        closeAlbum();
        ALBUM = new WorldScreenshotAlbum(worldId);
        ScreenshotMetadata.load(worldId);
        for (ScreenshotMetadata.Entry entry : ScreenshotMetadata.entries()) {
            GeotaggedScreenshot screenshot = new GeotaggedScreenshot(entry.name());
            if (screenshot.getFile(ImageType.ORIGINAL).isFile()) {
                ALBUM.add(screenshot);
            }
        }

        return true;
    }

    public static void load() {
        String worldId = ScreenshotMetadata.currentWorldId();
        if (worldId == null) {
            return;
        }

        boolean newWorld = loadAlbum(worldId);
        int screenshotsBeforeXaero = ALBUM.getScreenshots().size();
        if (MapCompat.hasXaero()) {
            xaeroSynchronized = XaeroWaypointUtil.loadExistingWaypoints();
        }

        if (newWorld || ALBUM.getScreenshots().size() > screenshotsBeforeXaero) {
            regenerateMissingThumbnails();
        }

        if (journeyRefresh != null) {
            journeyRefresh.run();
        }
    }

    public static void recordScreenshot(Vec3 position, File screenshotFile, File thumbnailFile) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        load();
        if (ALBUM == null) {
            return;
        }

        String name = screenshotFile.getName().replaceFirst("\\.[^.]+$", "");
        nonScreenshotWaypoints.remove(name);
        ScreenshotMetadata.saveScreenshot(name, position, minecraft.level.dimension());
        GeotaggedScreenshot screenshot = new GeotaggedScreenshot(name)
                .setFile(ImageType.ORIGINAL, screenshotFile)
                .setFile(ImageType.THUMBNAIL, thumbnailFile);
        ALBUM.add(screenshot);
        screenshot.getImageFuture(ImageType.THUMBNAIL);

        if (MapCompat.hasXaero()) {
            XaeroWaypointUtil.addNewScreenshotWaypoint(position, screenshotFile, thumbnailFile);
        }

        if (journeyRefresh != null) {
            journeyRefresh.run();
        }
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
                        if (journeyRefresh != null) {
                            journeyRefresh.run();
                        }
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
                Minecraft.getInstance().player.displayClientMessage(Component.translatable("message." + ModConstants.MODID + "." + key, count), false);
            }
        });
    }
}

package com.ccr4ft3r.geotaggedscreenshots;

import com.ccr4ft3r.geotaggedscreenshots.capture.ScreenshotCapture;
import com.ccr4ft3r.geotaggedscreenshots.config.ClientConfig;
import com.ccr4ft3r.geotaggedscreenshots.container.GeotaggedScreenshot;
import com.ccr4ft3r.geotaggedscreenshots.container.ImageType;
import com.ccr4ft3r.geotaggedscreenshots.container.WorldScreenshotAlbum;
import com.ccr4ft3r.geotaggedscreenshots.util.ImageUtil;
import com.ccr4ft3r.geotaggedscreenshots.util.xaero.XaeroWaypointUtil;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ScreenshotEvent;
import net.neoforged.neoforge.common.NeoForge;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.map.core.XaeroWorldMapCore;
import xaero.map.gui.GuiMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Mod(value = ModConstants.MODID, dist = Dist.CLIENT)
public class GeotaggedScreenshots {
    public static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    public static WorldScreenshotAlbum ALBUM;

    public GeotaggedScreenshots(IEventBus modEventBus, ModContainer modContainer) {
        migrateLegacyClientConfig();
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.CONFIG, ModConstants.MODID + "-client.toml");
        NeoForge.EVENT_BUS.addListener(GeotaggedScreenshots::onScreenshot);
        NeoForge.EVENT_BUS.addListener(GeotaggedScreenshots::onScreenOpening);
        NeoForge.EVENT_BUS.addListener(ScreenshotCapture::onFramePre);
        NeoForge.EVENT_BUS.addListener(ScreenshotCapture::onFramePost);
    }

    private static void migrateLegacyClientConfig() {
        Path configDirectory = Minecraft.getInstance().gameDirectory.toPath().resolve("config");
        Path legacyConfig = configDirectory.resolve("geotagged_screenshots-client.toml");
        Path modernConfig = configDirectory.resolve(ModConstants.MODID + "-client.toml");
        if (Files.isRegularFile(legacyConfig) && Files.notExists(modernConfig)) {
            try {
                Files.copy(legacyConfig, modernConfig);
            } catch (IOException e) {
                LogUtils.getLogger().warn("Couldn't migrate Geotagged Screenshots config", e);
            }
        }
    }

    private static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof GuiMap) {
            Minecraft.getInstance().execute(GeotaggedScreenshots::load);
        }
    }

    private static void onScreenshot(ScreenshotEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.isCanceled() || minecraft.player == null || minecraft.level == null) {
            return;
        }

        File screenshotFile;
        File thumbnailFile;
        Vec3 position = minecraft.player.position();

        try {
            screenshotFile = ImageUtil.saveScreenshot(event.getImage(), event.getScreenshotFile(), ClientConfig.CONFIG_DATA.useJpgForScreenshots.get());
            thumbnailFile = ImageUtil.createThumbnail(event.getImage(), screenshotFile);
        } catch (IOException e) {
            LogUtils.getLogger().error("Couldn't save geotagged screenshot", e);
            event.setResultMessage(Component.translatable("screenshot.failure", e.getMessage()));
            event.setCanceled(true);
            return;
        }

        if (ClientConfig.CONFIG_DATA.disableScreenshotChatMessage.get()) {
            event.setResultMessage(Component.empty());
        } else {
            Component fileLink = Component.literal(screenshotFile.getName()).withStyle(style ->
                    style.withUnderlined(true).withClickEvent(new ClickEvent.OpenFile(screenshotFile.toPath())));
            event.setResultMessage(Component.translatable("screenshot.success", fileLink));
        }

        event.setCanceled(true);
        minecraft.execute(() -> {
            load();
            if (ALBUM != null) {
                XaeroWaypointUtil.addNewScreenshotWaypoint(position, screenshotFile, thumbnailFile);
            }
        });
    }

    public static void load() {
        if (Minecraft.getInstance().level == null || XaeroWorldMapCore.currentSession == null) {
            return;
        }

        var processor = XaeroWorldMapCore.currentSession.getMapProcessor();
        if (processor == null || processor.getMapWorld() == null) {
            return;
        }

        String newWorldId = processor.getMapWorld().toString();
        WaypointSet screenshotSet = XaeroWaypointUtil.getScreenshotWaypointSet();
        if (screenshotSet == null) {
            return;
        }

        if (ALBUM == null || !ALBUM.getId().equals(newWorldId)) {
            if (ALBUM != null) {
                ALBUM.getScreenshots().forEach(GeotaggedScreenshot::close);
            }

            ALBUM = new WorldScreenshotAlbum(newWorldId);
        }

        int discovered = 0;
        for (var waypoint : screenshotSet.getWaypoints()) {
            if (ALBUM.getScreenshot(waypoint.getName()) == null) {
                ALBUM.add(new GeotaggedScreenshot(waypoint.getName()));
                discovered++;
            }
        }

        if (discovered > 0) {
            regenerateMissingThumbnails();
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
                    }

                    if (completed.incrementAndGet() == missing.size()) {
                        displayMessage("completed_thumbnail_regeneration", missing.size());
                    }
                });
            });
        }
    }

    private static void displayMessage(String key, int count) {
        if (!ClientConfig.CONFIG_DATA.displayThumbnailRegenerationInChat.get()) {
            return;
        }

        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().gui.getChat().addClientSystemMessage(Component.translatable("message." + ModConstants.MODID + "." + key, count));
            }
        });
    }
}

package com.ccr4ft3r.geotaggedscreenshots;

import com.ccr4ft3r.geotaggedscreenshots.config.ClientConfig;
import com.ccr4ft3r.geotaggedscreenshots.container.GeotaggedScreenshot;
import com.ccr4ft3r.geotaggedscreenshots.container.ImageType;
import com.ccr4ft3r.geotaggedscreenshots.container.WorldScreenshotAlbum;
import com.ccr4ft3r.geotaggedscreenshots.util.ImageUtil;
import com.ccr4ft3r.geotaggedscreenshots.util.xaero.XaeroWaypointUtil;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
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
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.CONFIG, ModConstants.MODID + "-client.toml");
        NeoForge.EVENT_BUS.addListener(GeotaggedScreenshots::onScreenshot);
        NeoForge.EVENT_BUS.addListener(GeotaggedScreenshots::onScreenOpening);
    }

    private static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof GuiMap) {
            Minecraft.getInstance().execute(GeotaggedScreenshots::load);
        }
    }

    private static void onScreenshot(ScreenshotEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        File screenshotFile = event.getScreenshotFile();
        Vec3 position = minecraft.player.position();
        File thumbnailFile = ImageUtil.createThumbnail(event.getImage(), screenshotFile);

        if (ClientConfig.CONFIG_DATA.disableScreenshotChatMessage.get()) {
            event.setResultMessage(Component.empty());
        }

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
        if (ALBUM != null && ALBUM.getId().equals(newWorldId)) {
            return;
        }

        WaypointSet screenshotSet = XaeroWaypointUtil.getScreenshotWaypointSet();
        if (screenshotSet == null) {
            return;
        }

        if (ALBUM != null) {
            ALBUM.getScreenshots().forEach(GeotaggedScreenshot::close);
        }

        ALBUM = new WorldScreenshotAlbum(newWorldId);
        screenshotSet.getWaypoints().forEach(waypoint -> ALBUM.add(new GeotaggedScreenshot(waypoint.getName())));
        regenerateMissingThumbnails();
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
                CompletableFuture<NativeImage> future = screenshot.getImageFuture(ImageType.ORIGINAL);
                if (future == null) {
                    return;
                }

                NativeImage image = future.join();
                if (image == null) {
                    return;
                }

                screenshot.setFile(ImageType.THUMBNAIL, ImageUtil.createThumbnail(image, screenshot.getFile(ImageType.ORIGINAL)));
                screenshot.close(ImageType.ORIGINAL);

                if (completed.incrementAndGet() == missing.size()) {
                    displayMessage("completed_thumbnail_regeneration", missing.size());
                }
            });
        }
    }

    private static void displayMessage(String key, int count) {
        if (!ClientConfig.CONFIG_DATA.displayThumbnailRegenerationInChat.get()) {
            return;
        }

        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(Component.translatable("message.geotagged_screenshots." + key, count), false);
            }
        });
    }
}

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
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ScreenshotEvent;
import net.neoforged.neoforge.common.NeoForge;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.map.gui.GuiMap;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Mod(value = ModConstants.MODID, dist = Dist.CLIENT)
public class GeotaggedScreenshots {
    public static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    public static WorldScreenshotAlbum ALBUM;
    private static WaypointSet synchronizedWaypointSet;
    private static int synchronizedWaypointCount = -1;
    private static int waypointScanTicks;

    public GeotaggedScreenshots(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.CONFIG, ModConstants.MODID + "-client.toml");
        NeoForge.EVENT_BUS.addListener(GeotaggedScreenshots::onScreenshot);
        NeoForge.EVENT_BUS.addListener(GeotaggedScreenshots::onScreenOpening);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, GeotaggedScreenshots::onClientTick);
        NeoForge.EVENT_BUS.addListener(ScreenshotCapture::onFramePre);
        NeoForge.EVENT_BUS.addListener(ScreenshotCapture::onFramePost);
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

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
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
        if (set == null) {
            return;
        }

        if (ALBUM == null || synchronizedWaypointSet != set || ++waypointScanTicks >= 20) {
            waypointScanTicks = 0;
            if (ALBUM == null || synchronizedWaypointSet != set || synchronizedWaypointCount != countWaypoints(set)) {
                load();
            }
        }
    }

    private static int countWaypoints(WaypointSet set) {
        int count = 0;
        for (var waypoint : set.getWaypoints()) {
            count++;
        }
        return count;
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
        synchronizedWaypointSet = null;
        synchronizedWaypointCount = -1;
        waypointScanTicks = 0;
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

        synchronizedWaypointSet = screenshotSet;
        synchronizedWaypointCount = countWaypoints(screenshotSet);

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

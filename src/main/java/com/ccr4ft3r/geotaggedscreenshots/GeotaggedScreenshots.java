package com.ccr4ft3r.geotaggedscreenshots;

import com.ccr4ft3r.geotaggedscreenshots.capture.ScreenshotCapture;
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
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.io.File;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Mod(value = ModConstants.MODID, dist = Dist.CLIENT)
public class GeotaggedScreenshots {
    public static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    public static WorldScreenshotAlbum ALBUM;
    private static ClientLevel activeLevel;
    private static Runnable journeyRefresh;
    private static final Set<GeotaggedScreenshot> regenerating = new HashSet<>();

    public GeotaggedScreenshots(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.CONFIG, ModConstants.MODID + "-client.toml");
        NeoForge.EVENT_BUS.addListener(GeotaggedScreenshots::onScreenOpening);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, GeotaggedScreenshots::onClientTick);
        NeoForge.EVENT_BUS.addListener(ScreenshotCapture::onFramePre);
        NeoForge.EVENT_BUS.addListener(ScreenshotCapture::onFramePost);
    }

    private static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() != null && event.getNewScreen().getClass().getName().equals("xaero.map.gui.GuiMap")) {
            Minecraft.getInstance().execute(GeotaggedScreenshots::load);
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            if (ALBUM != null) {
                closeAlbum();
            }
            return;
        }

        if (ALBUM == null || activeLevel != client.level) {
            load();
            return;
        }

        if (MapCompat.hasXaero() && XaeroWaypointUtil.needsSync()) {
            load();
        }
    }

    private static void closeAlbum() {
        if (ALBUM != null) {
            ALBUM.getScreenshots().forEach(GeotaggedScreenshot::close);
            ALBUM = null;
        }

        activeLevel = null;
        regenerating.clear();
        if (MapCompat.hasXaero()) {
            XaeroWaypointUtil.reset();
        }
    }

    public static void setJourneyRefresh(Runnable callback) {
        journeyRefresh = callback;
    }

    private static void refreshJourneyMap() {
        if (journeyRefresh != null) {
            journeyRefresh.run();
        }
    }

    public static void load() {
        Minecraft client = Minecraft.getInstance();
        String worldId = ScreenshotMetadata.currentWorldId();
        if (worldId == null) {
            return;
        }

        boolean newWorld = ALBUM == null || !ALBUM.getId().equals(worldId);
        if (newWorld) {
            closeAlbum();
            ALBUM = new WorldScreenshotAlbum(worldId);
            ScreenshotMetadata.load(worldId);
            for (ScreenshotMetadata.Entry entry : ScreenshotMetadata.entries()) {
                GeotaggedScreenshot screenshot = new GeotaggedScreenshot(entry.name());
                if (screenshot.getFile(ImageType.ORIGINAL).isFile()) {
                    ALBUM.add(screenshot);
                    screenshot.getImageFuture(ImageType.THUMBNAIL);
                }
            }
        }

        activeLevel = client.level;
        int before = ALBUM.getScreenshots().size();
        if (MapCompat.hasXaero()) {
            XaeroWaypointUtil.loadExistingWaypoints();
        }

        if (newWorld || ALBUM.getScreenshots().size() > before) {
            regenerateMissingThumbnails();
        }
        refreshJourneyMap();
    }

    public static void recordScreenshot(Vec3 position, File screenshotFile, File thumbnailFile) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }

        load();
        if (ALBUM == null) {
            return;
        }

        String name = screenshotFile.getName().replaceFirst("\\.[^.]+$", "");
        ScreenshotMetadata.saveScreenshot(name, position, client.level.dimension());
        GeotaggedScreenshot screenshot = new GeotaggedScreenshot(name)
                .setFile(ImageType.ORIGINAL, screenshotFile)
                .setFile(ImageType.THUMBNAIL, thumbnailFile);
        ALBUM.add(screenshot);
        screenshot.getImageFuture(ImageType.THUMBNAIL);

        if (MapCompat.hasXaero()) {
            XaeroWaypointUtil.loadExistingWaypoints();
        }
        refreshJourneyMap();
    }

    private static void regenerateMissingThumbnails() {
        List<GeotaggedScreenshot> missing = ALBUM.getScreenshots().stream()
                .filter(screenshot -> !screenshot.getFile(ImageType.THUMBNAIL).exists()
                        && screenshot.getFile(ImageType.ORIGINAL).exists()
                        && !regenerating.contains(screenshot))
                .toList();

        if (missing.isEmpty()) {
            return;
        }

        AtomicInteger completed = new AtomicInteger();
        LogUtils.getLogger().info("Regenerating thumbnails for {} screenshots", missing.size());
        displayMessage("started_thumbnail_regeneration", missing.size());

        for (GeotaggedScreenshot screenshot : missing) {
            regenerating.add(screenshot);
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
                    regenerating.remove(screenshot);
                    if (ALBUM == null || ALBUM.getScreenshot(screenshot.getName()) != screenshot) {
                        return;
                    }
                    if (generatedThumbnail != null && generatedThumbnail.isFile()) {
                        screenshot.setFile(ImageType.THUMBNAIL, generatedThumbnail);
                        screenshot.getImageFuture(ImageType.THUMBNAIL);
                        refreshJourneyMap();
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

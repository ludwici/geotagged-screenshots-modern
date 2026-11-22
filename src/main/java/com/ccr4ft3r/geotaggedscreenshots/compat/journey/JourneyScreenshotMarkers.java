package com.ccr4ft3r.geotaggedscreenshots.compat.journey;

import com.ccr4ft3r.geotaggedscreenshots.GeotaggedScreenshots;
import com.ccr4ft3r.geotaggedscreenshots.ModConstants;
import com.ccr4ft3r.geotaggedscreenshots.container.GeotaggedScreenshot;
import com.ccr4ft3r.geotaggedscreenshots.container.ImageType;
import com.ccr4ft3r.geotaggedscreenshots.container.ScreenshotMetadata;
import com.ccr4ft3r.geotaggedscreenshots.screens.ScreenshotView;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.display.DisplayType;
import journeymap.api.v2.client.display.IOverlayListener;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.model.MapImage;
import journeymap.api.v2.client.util.UIState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class JourneyScreenshotMarkers {
    private static IClientAPI api;
    private static boolean mappingActive;
    private static String activeWorld;
    private static final Map<String, MarkerOverlay> MARKERS = new HashMap<>();
    private static final Set<String> PENDING = new HashSet<>();

    private JourneyScreenshotMarkers() {
    }

    public static void initialize(IClientAPI clientApi) {
        api = clientApi;
    }

    public static void beginMapping() {
        clear();
        mappingActive = true;
    }

    public static void clear() {
        mappingActive = false;
        activeWorld = null;
        MARKERS.clear();
        PENDING.clear();
        if (api != null) {
            api.removeAll(ModConstants.MODID);
        }
    }

    public static void refresh() {
        if (!mappingActive || api == null || GeotaggedScreenshots.ALBUM == null || Minecraft.getInstance().level == null) {
            return;
        }

        if (!api.playerAccepts(ModConstants.MODID, DisplayType.Marker)) {
            return;
        }

        String worldId = GeotaggedScreenshots.ALBUM.getId();
        if (!worldId.equals(activeWorld)) {
            MARKERS.clear();
            PENDING.clear();
            api.removeAll(ModConstants.MODID);
            activeWorld = worldId;
        }

        for (ScreenshotMetadata.Entry entry : ScreenshotMetadata.entries()) {
            if (!MARKERS.containsKey(entry.name())) {
                GeotaggedScreenshot screenshot = GeotaggedScreenshots.ALBUM.getScreenshot(entry.name());
                if (screenshot != null) {
                    showWhenReady(entry, screenshot);
                }
            }
        }
    }

    private static void showWhenReady(ScreenshotMetadata.Entry entry, GeotaggedScreenshot screenshot) {
        CompletableFuture<NativeImage> future = screenshot.getImageFuture(ImageType.THUMBNAIL);
        if (future == null) {
            return;
        }

        if (!future.isDone()) {
            if (PENDING.add(entry.name())) {
                future.whenComplete((image, exception) -> Minecraft.getInstance().execute(() -> {
                    PENDING.remove(entry.name());
                    if (mappingActive && image != null && GeotaggedScreenshots.ALBUM != null
                            && GeotaggedScreenshots.ALBUM.getScreenshot(entry.name()) == screenshot) {
                        showWhenReady(entry, screenshot);
                    }
                }));
            }
            return;
        }

        NativeImage image = future.getNow(null);
        if (image == null || MARKERS.containsKey(entry.name())) {
            return;
        }

        ResourceLocation texture = screenshot.getId(ImageType.THUMBNAIL);
        if (texture == null) {
            return;
        }

        int width = 44;
        int height = Math.max(1, Math.round(width * (float) image.getHeight() / image.getWidth()));
        MapImage icon = new MapImage(texture, image.getWidth(), image.getHeight());
        icon.setDisplayWidth(width);
        icon.setDisplayHeight(height);
        icon.centerAnchors();

        ResourceKey<Level> dimension;
        try {
            dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(entry.dimension()));
        } catch (IllegalArgumentException exception) {
            LogUtils.getLogger().warn("Invalid dimension for screenshot {}", entry.name(), exception);
            return;
        }

        MarkerOverlay marker = new MarkerOverlay(ModConstants.MODID, new BlockPos(entry.x(), entry.y(), entry.z()), icon);
        marker.setDimension(dimension);
        marker.setTitle(entry.name());
        marker.setOverlayGroupName("Screenshots");
        marker.setActiveUIs(Context.UI.Fullscreen, Context.UI.Minimap);
        marker.setOverlayListener(new IOverlayListener() {
            @Override
            public boolean onMouseClick(UIState state, Point2D.Double mouse, BlockPos block, int button, boolean doubleClick) {
                if (button != 0) {
                    return true;
                }

                Minecraft minecraft = Minecraft.getInstance();
                minecraft.setScreen(new ScreenshotView(screenshot, new Vec3(entry.x(), entry.y(), entry.z()), minecraft.screen));
                return false;
            }
        });

        try {
            api.show(marker);
            MARKERS.put(entry.name(), marker);
        } catch (Exception exception) {
            LogUtils.getLogger().error("Could not show JourneyMap screenshot marker {}", entry.name(), exception);
        }
    }
}

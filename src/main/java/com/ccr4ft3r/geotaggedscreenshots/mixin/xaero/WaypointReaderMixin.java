package com.ccr4ft3r.geotaggedscreenshots.mixin.xaero;

import com.ccr4ft3r.geotaggedscreenshots.GeotaggedScreenshots;
import com.ccr4ft3r.geotaggedscreenshots.container.GeotaggedScreenshot;
import com.ccr4ft3r.geotaggedscreenshots.container.ImageType;
import com.ccr4ft3r.geotaggedscreenshots.util.ThumbnailGeometry;
import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.mods.gui.Waypoint;
import xaero.map.mods.gui.WaypointReader;
import xaero.map.mods.gui.WaypointRenderContext;

import java.util.concurrent.CompletableFuture;

@Mixin(WaypointReader.class)
public abstract class WaypointReaderMixin {
    @Unique
    private static NativeImage geotagged_screenshots$getThumbnail(Waypoint waypoint) {
        if (GeotaggedScreenshots.ALBUM == null) {
            return null;
        }

        GeotaggedScreenshot screenshot = GeotaggedScreenshots.ALBUM.getScreenshot(waypoint.getName());
        if (screenshot == null) {
            return null;
        }

        CompletableFuture<NativeImage> future = screenshot.getImageFuture(ImageType.THUMBNAIL);
        if (future == null || !future.isDone()) {
            return null;
        }

        return future.getNow(null);
    }

    @Inject(method = "getInteractionBoxRight(Lxaero/map/mods/gui/Waypoint;Lxaero/map/mods/gui/WaypointRenderContext;F)I", at = @At("HEAD"), remap = false, cancellable = true)
    private void geotagged_screenshots$expandRight(Waypoint waypoint, WaypointRenderContext context, float scale, CallbackInfoReturnable<Integer> cir) {
        NativeImage image = geotagged_screenshots$getThumbnail(waypoint);
        if (image != null) {
            cir.setReturnValue((ThumbnailGeometry.worldMapWidth(image.getWidth(), scale) + 1) / 2);
        }
    }

    @Inject(method = "getInteractionBoxTop(Lxaero/map/mods/gui/Waypoint;Lxaero/map/mods/gui/WaypointRenderContext;F)I", at = @At("HEAD"), remap = false, cancellable = true)
    private void geotagged_screenshots$expandTop(Waypoint waypoint, WaypointRenderContext context, float scale, CallbackInfoReturnable<Integer> cir) {
        NativeImage image = geotagged_screenshots$getThumbnail(waypoint);
        if (image != null) {
            cir.setReturnValue(-((ThumbnailGeometry.worldMapHeight(image.getWidth(), image.getHeight(), scale) + 1) / 2));
        }
    }

    @Inject(method = "getInteractionBoxBottom(Lxaero/map/mods/gui/Waypoint;Lxaero/map/mods/gui/WaypointRenderContext;F)I", at = @At("HEAD"), remap = false, cancellable = true)
    private void geotagged_screenshots$expandBottom(Waypoint waypoint, WaypointRenderContext context, float scale, CallbackInfoReturnable<Integer> cir) {
        NativeImage image = geotagged_screenshots$getThumbnail(waypoint);
        if (image != null) {
            cir.setReturnValue((ThumbnailGeometry.worldMapHeight(image.getWidth(), image.getHeight(), scale) + 1) / 2);
        }
    }
}

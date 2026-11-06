package com.ccr4ft3r.geotaggedscreenshots.mixin.xaero;

import com.ccr4ft3r.geotaggedscreenshots.GeotaggedScreenshots;
import com.ccr4ft3r.geotaggedscreenshots.container.GeotaggedScreenshot;
import com.ccr4ft3r.geotaggedscreenshots.container.ImageType;
import com.ccr4ft3r.geotaggedscreenshots.util.RenderUtil;
import com.ccr4ft3r.geotaggedscreenshots.util.ThumbnailGeometry;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.render.WaypointMapRenderer;

import java.util.concurrent.CompletableFuture;

@Mixin(WaypointMapRenderer.class)
public abstract class WaypointMapRendererMixin {
    @Inject(method = "drawIconOnGUI(Lnet/minecraft/client/gui/GuiGraphics;Lxaero/common/minimap/render/MinimapRendererHelper;Lxaero/common/minimap/waypoints/Waypoint;IIILnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V", at = @At("HEAD"), remap = false, cancellable = true)
    private void geotagged_screenshots$renderThumbnail(GuiGraphics graphics, MinimapRendererHelper helper, Waypoint waypoint, int x, int y, int opacity, MultiBufferSource.BufferSource buffers, VertexConsumer background, VertexConsumer icon, CallbackInfo ci) {
        if (GeotaggedScreenshots.ALBUM == null) {
            return;
        }

        GeotaggedScreenshot screenshot = GeotaggedScreenshots.ALBUM.getScreenshot(waypoint.getName());
        if (screenshot == null) {
            return;
        }

        CompletableFuture<NativeImage> future = screenshot.getImageFuture(ImageType.THUMBNAIL);
        if (future == null || !future.isDone()) {
            return;
        }

        NativeImage image = future.getNow(null);
        if (image == null) {
            return;
        }

        ResourceLocation texture = screenshot.getId(ImageType.THUMBNAIL);
        if (texture == null) {
            return;
        }

        int width = ThumbnailGeometry.minimapWidth();
        int height = ThumbnailGeometry.minimapHeight(image.getWidth(), image.getHeight());
        RenderUtil.renderImage(graphics, texture, image.getWidth(), image.getHeight(), width, height, x - width / 2, y - height / 2);
        ci.cancel();
    }
}

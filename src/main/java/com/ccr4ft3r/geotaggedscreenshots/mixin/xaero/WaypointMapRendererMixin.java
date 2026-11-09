package com.ccr4ft3r.geotaggedscreenshots.mixin.xaero;

import com.ccr4ft3r.geotaggedscreenshots.GeotaggedScreenshots;
import com.ccr4ft3r.geotaggedscreenshots.container.GeotaggedScreenshot;
import com.ccr4ft3r.geotaggedscreenshots.container.ImageType;
import com.ccr4ft3r.geotaggedscreenshots.util.ThumbnailGeometry;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.element.render.MinimapElementGraphics;
import xaero.hud.minimap.waypoint.render.WaypointMapRenderer;
import xaero.lib.client.graphics.XaeroBufferProvider;

import java.util.concurrent.CompletableFuture;

@Mixin(WaypointMapRenderer.class)
public abstract class WaypointMapRendererMixin {
    @Inject(method = "drawIcon(Lxaero/hud/minimap/element/render/MinimapElementGraphics;Lxaero/common/minimap/render/MinimapRendererHelper;Lxaero/common/minimap/waypoints/Waypoint;IIILxaero/lib/client/graphics/XaeroBufferProvider;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V", at = @At("HEAD"), remap = false, cancellable = true)
    private void geotagged_screenshots$renderThumbnail(MinimapElementGraphics graphics, MinimapRendererHelper helper, Waypoint waypoint, int x, int y, int opacity, XaeroBufferProvider provider, VertexConsumer background, VertexConsumer icon, CallbackInfo ci) {
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

        Identifier texture = screenshot.getId(ImageType.THUMBNAIL);
        if (texture == null) {
            return;
        }

        int width = ThumbnailGeometry.minimapWidth();
        int height = ThumbnailGeometry.minimapHeight(image.getWidth(), image.getHeight());
        graphics.blit(texture, x - width / 2, y - height / 2, 0, 0, width, height, 1, 1, 1, RenderPipelines.GUI_TEXTURED);
        ci.cancel();
    }
}

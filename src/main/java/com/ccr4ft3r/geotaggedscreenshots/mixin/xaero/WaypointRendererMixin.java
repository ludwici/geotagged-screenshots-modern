package com.ccr4ft3r.geotaggedscreenshots.mixin.xaero;

import com.ccr4ft3r.geotaggedscreenshots.GeotaggedScreenshots;
import com.ccr4ft3r.geotaggedscreenshots.container.GeotaggedScreenshot;
import com.ccr4ft3r.geotaggedscreenshots.container.ImageType;
import com.ccr4ft3r.geotaggedscreenshots.util.ThumbnailGeometry;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import xaero.lib.client.graphics.XaeroBufferProvider;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.element.MapElementGraphics;
import xaero.map.element.render.ElementReader;
import xaero.map.element.render.ElementRenderProvider;
import xaero.map.element.render.ElementRenderer;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.map.mods.gui.Waypoint;
import xaero.map.mods.gui.WaypointRenderContext;
import xaero.map.mods.gui.WaypointRenderer;

import java.util.concurrent.CompletableFuture;

@Mixin(WaypointRenderer.class)
public abstract class WaypointRendererMixin extends ElementRenderer<Waypoint, WaypointRenderContext, WaypointRenderer> {
    protected WaypointRendererMixin(WaypointRenderContext context, ElementRenderProvider<Waypoint, WaypointRenderContext> provider, ElementReader<Waypoint, WaypointRenderContext, WaypointRenderer> reader) {
        super(context, provider, reader);
    }

    @Inject(method = "renderElement(Lxaero/map/mods/gui/Waypoint;ZDFDDLxaero/map/element/render/ElementRenderInfo;Lxaero/map/element/MapElementGraphics;Lxaero/lib/client/graphics/XaeroBufferProvider;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)Z", at = @At("HEAD"), remap = false, cancellable = true)
    private void renderScreenshot(Waypoint waypoint, boolean hovered, double depth, float scale, double partialX, double partialY, ElementRenderInfo info, MapElementGraphics graphics, XaeroBufferProvider buffers, MultiTextureRenderTypeRendererProvider renderer, CallbackInfoReturnable<Boolean> cir) {
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

        int width = ThumbnailGeometry.worldMapWidth(image.getWidth(), scale);
        int height = ThumbnailGeometry.worldMapHeight(image.getWidth(), image.getHeight(), scale);
        Identifier id = screenshot.getId(ImageType.THUMBNAIL);
        if (id == null) {
            return;
        }

        PoseStack pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(partialX, partialY, 0.0D);
            float waypointScale = scale * this.context.worldmapWaypointsScale;
            pose.scale(waypointScale, waypointScale, 1.0F);
            graphics.blit(id, -width / 2, -height / 2, 0, 1, width, height, 1, 0, 1, RenderPipelines.GUI_TEXTURED);
        } finally {
            pose.popPose();
        }

        cir.setReturnValue(false);
    }

    @Inject(method = "renderElementShadow(Lxaero/map/mods/gui/Waypoint;ZFDDLxaero/map/element/render/ElementRenderInfo;Lxaero/map/element/MapElementGraphics;Lxaero/lib/client/graphics/XaeroBufferProvider;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)V", at = @At("HEAD"), remap = false, cancellable = true)
    private void hideShadow(Waypoint waypoint, boolean hovered, float scale, double partialX, double partialY, ElementRenderInfo info, MapElementGraphics graphics, XaeroBufferProvider buffers, MultiTextureRenderTypeRendererProvider renderer, CallbackInfo ci) {
        if (GeotaggedScreenshots.ALBUM != null && GeotaggedScreenshots.ALBUM.getScreenshot(waypoint.getName()) != null) {
            ci.cancel();
        }
    }
}

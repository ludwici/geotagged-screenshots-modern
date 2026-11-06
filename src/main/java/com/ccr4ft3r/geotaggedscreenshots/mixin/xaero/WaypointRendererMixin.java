package com.ccr4ft3r.geotaggedscreenshots.mixin.xaero;

import com.ccr4ft3r.geotaggedscreenshots.GeotaggedScreenshots;
import com.ccr4ft3r.geotaggedscreenshots.container.GeotaggedScreenshot;
import com.ccr4ft3r.geotaggedscreenshots.container.ImageType;
import com.ccr4ft3r.geotaggedscreenshots.screens.ScreenshotView;
import com.ccr4ft3r.geotaggedscreenshots.util.RenderUtil;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.element.MapElementReader;
import xaero.map.element.MapElementRenderProvider;
import xaero.map.element.MapElementRenderer;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.map.gui.GuiMap;
import xaero.map.mods.gui.Waypoint;
import xaero.map.mods.gui.WaypointRenderContext;
import xaero.map.mods.gui.WaypointRenderer;

import java.util.concurrent.CompletableFuture;

@Mixin(WaypointRenderer.class)
public abstract class WaypointRendererMixin extends MapElementRenderer<Waypoint, WaypointRenderContext, WaypointRenderer> {
    @Unique
    private static boolean geotagged_screenshots$mousePressed;

    protected WaypointRendererMixin(WaypointRenderContext context, MapElementRenderProvider<Waypoint, WaypointRenderContext> provider, MapElementReader<Waypoint, WaypointRenderContext, WaypointRenderer> reader) {
        super(context, provider, reader);
    }

    @Inject(method = "renderElement(Lxaero/map/mods/gui/Waypoint;ZDFDDLxaero/map/element/render/ElementRenderInfo;Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)Z", at = @At("HEAD"), remap = false, cancellable = true)
    private void renderScreenshot(Waypoint waypoint, boolean hovered, double depth, float scale, double partialX, double partialY, ElementRenderInfo info, GuiGraphics graphics, MultiBufferSource.BufferSource buffers, MultiTextureRenderTypeRendererProvider renderer, CallbackInfoReturnable<Boolean> cir) {
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

        int width = Math.max(24, Math.min(144, (int) (image.getWidth() * scale / 6.0F)));
        int height = Math.max(16, (int) ((double) image.getHeight() * width / image.getWidth()));
        RenderUtil.renderImage(graphics, screenshot.getId(ImageType.THUMBNAIL), image.getWidth(), image.getHeight(), width, height, -width / 2, -height / 2);

        Minecraft minecraft = Minecraft.getInstance();
        boolean down = GLFW.glfwGetMouseButton(minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (!down) {
            geotagged_screenshots$mousePressed = false;
        } else if (hovered && !geotagged_screenshots$mousePressed && minecraft.screen instanceof GuiMap) {
            geotagged_screenshots$mousePressed = true;
            minecraft.setScreen(new ScreenshotView(screenshot, new Vec3(waypoint.getX(), waypoint.getY(), waypoint.getZ()), minecraft.screen));
        }

        cir.setReturnValue(false);
    }

    @Inject(method = "renderElementShadow(Lxaero/map/mods/gui/Waypoint;ZFDDLxaero/map/element/render/ElementRenderInfo;Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)V", at = @At("HEAD"), remap = false, cancellable = true)
    private void hideShadow(Waypoint waypoint, boolean hovered, float scale, double partialX, double partialY, ElementRenderInfo info, GuiGraphics graphics, MultiBufferSource.BufferSource buffers, MultiTextureRenderTypeRendererProvider renderer, CallbackInfo ci) {
        if (GeotaggedScreenshots.ALBUM != null && GeotaggedScreenshots.ALBUM.getScreenshot(waypoint.getName()) != null) {
            ci.cancel();
        }
    }
}

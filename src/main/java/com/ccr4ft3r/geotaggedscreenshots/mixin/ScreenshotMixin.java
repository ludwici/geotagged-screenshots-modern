package com.ccr4ft3r.geotaggedscreenshots.mixin;

import com.ccr4ft3r.geotaggedscreenshots.capture.ScreenshotCapture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.function.Consumer;

@Mixin(Screenshot.class)
public abstract class ScreenshotMixin {
    @Inject(method = "grab(Ljava/io/File;Lcom/mojang/blaze3d/pipeline/RenderTarget;Ljava/util/function/Consumer;)V", at = @At("HEAD"), cancellable = true)
    private static void geotagged_screenshots$captureWithoutHud(File gameDirectory, RenderTarget target, Consumer<Component> messageConsumer, CallbackInfo ci) {
        if (ScreenshotCapture.intercept(gameDirectory, target, messageConsumer)) {
            ci.cancel();
        }
    }
}

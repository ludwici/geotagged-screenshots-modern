package com.ccr4ft3r.geotaggedscreenshots.mixin;

import com.ccr4ft3r.geotaggedscreenshots.GeotaggedScreenshots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "setScreen", at = @At("TAIL"))
    private void geotagged_screenshots$onMapOpened(Screen screen, CallbackInfo ci) {
        if (screen != null && screen.getClass().getName().equals("xaero.map.gui.GuiMap")) {
            GeotaggedScreenshots.load();
        }
    }
}

package com.ccr4ft3r.geotaggedscreenshots.mixin.xaero;

import com.ccr4ft3r.geotaggedscreenshots.GeotaggedScreenshots;
import com.ccr4ft3r.geotaggedscreenshots.container.GeotaggedScreenshot;
import com.ccr4ft3r.geotaggedscreenshots.screens.ScreenshotView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.element.HoveredMapElementHolder;
import xaero.map.gui.GuiMap;
import xaero.map.mods.gui.Waypoint;

@Mixin(GuiMap.class)
public abstract class GuiMapClickMixin {
    @Shadow(remap = false)
    private HoveredMapElementHolder<?, ?> viewed;

    @Inject(method = "mapClicked(III)V", at = @At("HEAD"), remap = false, cancellable = true)
    private void geotagged_screenshots$openScreenshot(int button, int mouseX, int mouseY, CallbackInfo ci) {
        if (button != 0 || viewed == null || !(viewed.getElement() instanceof Waypoint waypoint) || GeotaggedScreenshots.ALBUM == null) {
            return;
        }

        GeotaggedScreenshot screenshot = GeotaggedScreenshots.ALBUM.getScreenshot(waypoint.getName());
        if (screenshot == null) {
            return;
        }

        Screen parent = (Screen) (Object) this;
        Minecraft.getInstance().setScreen(new ScreenshotView(screenshot, new Vec3(waypoint.getX(), waypoint.getY(), waypoint.getZ()), parent));
        ci.cancel();
    }
}

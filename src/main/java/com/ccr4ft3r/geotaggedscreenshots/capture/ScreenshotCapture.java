package com.ccr4ft3r.geotaggedscreenshots.capture;

import com.ccr4ft3r.geotaggedscreenshots.config.ClientConfig;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.ClickEvent;
import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.platform.NativeImage;
import com.ccr4ft3r.geotaggedscreenshots.util.xaero.XaeroWaypointUtil;
import com.ccr4ft3r.geotaggedscreenshots.util.ImageUtil;
import com.ccr4ft3r.geotaggedscreenshots.container.WorldScreenshotAlbum;
import com.ccr4ft3r.geotaggedscreenshots.GeotaggedScreenshots;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.io.File;
import java.util.Locale;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ScreenshotCapture {
    private static final List<Request> REQUESTS = new ArrayList<>();
    private static boolean capturing;
    private static boolean frameReady;
    private static boolean previousHideGui;

    private ScreenshotCapture() {
    }

    public static boolean intercept(File gameDirectory, RenderTarget renderTarget, Consumer<Component> messageConsumer) {
        Minecraft minecraft = Minecraft.getInstance();
        if (capturing || minecraft.level == null || minecraft.gui.screen() != null || !ClientConfig.CONFIG_DATA.hideUiAtTakingScreenshots.get()) {
            return false;
        }

        if (REQUESTS.isEmpty()) {
            previousHideGui = minecraft.gui.hud.isHidden();
            if (!previousHideGui) {
                minecraft.gui.hud.toggle();
            }
        }

        REQUESTS.add(new Request(gameDirectory, renderTarget, messageConsumer));
        return true;
    }

    public static void onFramePre(RenderFrameEvent.Pre event) {
        if (!REQUESTS.isEmpty()) {
            frameReady = true;
        }
    }

    public static void onFramePost(RenderFrameEvent.Post event) {
        if (!frameReady || REQUESTS.isEmpty()) {
            return;
        }

        List<Request> ready = new ArrayList<>(REQUESTS);
        REQUESTS.clear();
        frameReady = false;
        capturing = true;

        try {
            for (Request request : ready) {
                Screenshot.grab(request.gameDirectory(), request.renderTarget(), request.messageConsumer());
            }
        } finally {
            capturing = false;
            if (Minecraft.getInstance().gui.hud.isHidden() != previousHideGui) {
                Minecraft.getInstance().gui.hud.toggle();
            }
        }
    }

    public static boolean capture(File gameDirectory, String screenshotName, RenderTarget renderTarget, int screenshotScale, Consumer<Component> messageConsumer) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return false;
        }


        Vec3 position = minecraft.player.position();
        Level sourceLevel = minecraft.level;
        File screenshotDirectory = new File(gameDirectory, "screenshots");
        String name = screenshotName != null ? new File(screenshotName).getName() : new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss", Locale.ROOT).format(new Date());
        File requestedFile = new File(screenshotDirectory, name.endsWith(".png") ? name : name + ".png");

        Screenshot.takeScreenshot(renderTarget, screenshotScale, image -> GeotaggedScreenshots.EXECUTOR.execute(() -> {
            try (image) {
                File screenshotFile = ImageUtil.saveScreenshot(image, requestedFile, ClientConfig.CONFIG_DATA.useJpgForScreenshots.get());
                File thumbnailFile = ImageUtil.createThumbnail(image, screenshotFile);

                minecraft.execute(() -> {
                    if (minecraft.level == sourceLevel) {
                        GeotaggedScreenshots.load();
                        WorldScreenshotAlbum album = GeotaggedScreenshots.ALBUM;
                        if (album != null) {
                            XaeroWaypointUtil.addNewScreenshotWaypoint(position, screenshotFile, thumbnailFile);
                        }
                    }

                    if (!ClientConfig.CONFIG_DATA.disableScreenshotChatMessage.get()) {
                        Component fileLink = Component.literal(screenshotFile.getName()).withStyle(style ->
                                style.withUnderlined(true).withClickEvent(new ClickEvent.OpenFile(screenshotFile.toPath())));
                        messageConsumer.accept(Component.translatable("screenshot.success", fileLink));
                    }
                });
            } catch (IOException e) {
                LogUtils.getLogger().error("Couldn't save geotagged screenshot", e);
                minecraft.execute(() -> messageConsumer.accept(Component.translatable("screenshot.failure", e.getMessage())));
            }
        }));

        return true;
    }

    private record Request(File gameDirectory, RenderTarget renderTarget, Consumer<Component> messageConsumer) {
    }
}

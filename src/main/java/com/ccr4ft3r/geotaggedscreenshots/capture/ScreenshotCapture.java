package com.ccr4ft3r.geotaggedscreenshots.capture;

import com.ccr4ft3r.geotaggedscreenshots.GeotaggedScreenshots;
import com.ccr4ft3r.geotaggedscreenshots.config.ClientConfig;
import com.ccr4ft3r.geotaggedscreenshots.container.WorldScreenshotAlbum;
import com.ccr4ft3r.geotaggedscreenshots.util.ImageUtil;
import com.ccr4ft3r.geotaggedscreenshots.util.xaero.XaeroWaypointUtil;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class ScreenshotCapture {
    private static final List<Request> REQUESTS = new ArrayList<>();
    private static boolean capturing;
    private static boolean previousHideGui;

    private ScreenshotCapture() {
    }

    public static boolean intercept(File gameDirectory, RenderTarget renderTarget, Consumer<Component> messageConsumer) {
        Minecraft minecraft = Minecraft.getInstance();
        if (capturing || minecraft.level == null || minecraft.screen != null || !ClientConfig.CONFIG_DATA.hideUiAtTakingScreenshots) {
            return false;
        }

        if (REQUESTS.isEmpty()) {
            previousHideGui = minecraft.options.hideGui;
            minecraft.options.hideGui = true;
        }

        REQUESTS.add(new Request(gameDirectory, renderTarget, messageConsumer));
        return true;
    }

    public static void onFrameRendered() {
        if (REQUESTS.isEmpty()) {
            return;
        }

        List<Request> ready = new ArrayList<>(REQUESTS);
        REQUESTS.clear();
        capturing = true;

        try {
            for (Request request : ready) {
                Screenshot.grab(request.gameDirectory(), request.renderTarget(), request.messageConsumer());
            }
        } finally {
            capturing = false;
            Minecraft.getInstance().options.hideGui = previousHideGui;
        }
    }

    public static boolean capture(File gameDirectory, String screenshotName, RenderTarget renderTarget, Consumer<Component> messageConsumer) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return false;
        }

        NativeImage image = Screenshot.takeScreenshot(renderTarget);
        Vec3 position = minecraft.player.position();
        Level sourceLevel = minecraft.level;
        File screenshotDirectory = new File(gameDirectory, "screenshots");
        String name = screenshotName != null ? new File(screenshotName).getName() : new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss", Locale.ROOT).format(new Date());
        File requestedFile = new File(screenshotDirectory, name.endsWith(".png") ? name : name + ".png");

        GeotaggedScreenshots.EXECUTOR.execute(() -> {
            try (image) {
                File screenshotFile = ImageUtil.saveScreenshot(image, requestedFile, ClientConfig.CONFIG_DATA.useJpgForScreenshots);
                File thumbnailFile = ImageUtil.createThumbnail(image, screenshotFile);

                minecraft.execute(() -> {
                    if (minecraft.level == sourceLevel) {
                        GeotaggedScreenshots.load();
                        WorldScreenshotAlbum album = GeotaggedScreenshots.ALBUM;
                        if (album != null) {
                            XaeroWaypointUtil.addNewScreenshotWaypoint(position, screenshotFile, thumbnailFile);
                        }
                    }

                    if (!ClientConfig.CONFIG_DATA.disableScreenshotChatMessage) {
                        Component fileLink = Component.literal(screenshotFile.getName()).withStyle(style ->
                                style.withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, screenshotFile.getAbsolutePath())));
                        messageConsumer.accept(Component.translatable("screenshot.success", fileLink));
                    }
                });
            } catch (IOException e) {
                LogUtils.getLogger().error("Couldn't save geotagged screenshot", e);
                minecraft.execute(() -> messageConsumer.accept(Component.translatable("screenshot.failure", e.getMessage())));
            }
        });

        return true;
    }

    private record Request(File gameDirectory, RenderTarget renderTarget, Consumer<Component> messageConsumer) {
    }
}

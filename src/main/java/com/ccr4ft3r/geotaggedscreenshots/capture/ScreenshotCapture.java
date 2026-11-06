package com.ccr4ft3r.geotaggedscreenshots.capture;

import com.ccr4ft3r.geotaggedscreenshots.config.ClientConfig;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.io.File;
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
        if (capturing || minecraft.level == null || minecraft.screen != null || !ClientConfig.CONFIG_DATA.hideUiAtTakingScreenshots.get()) {
            return false;
        }

        if (REQUESTS.isEmpty()) {
            previousHideGui = minecraft.options.hideGui;
            minecraft.options.hideGui = true;
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
            Minecraft.getInstance().options.hideGui = previousHideGui;
        }
    }

    private record Request(File gameDirectory, RenderTarget renderTarget, Consumer<Component> messageConsumer) {
    }
}

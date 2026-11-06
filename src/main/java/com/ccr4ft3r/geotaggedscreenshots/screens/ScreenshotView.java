package com.ccr4ft3r.geotaggedscreenshots.screens;

import com.ccr4ft3r.geotaggedscreenshots.container.GeotaggedScreenshot;
import com.ccr4ft3r.geotaggedscreenshots.container.ImageType;
import com.ccr4ft3r.geotaggedscreenshots.util.RenderUtil;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.CompletableFuture;

public class ScreenshotView extends Screen {
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 52;
    private static final int OFFSET = 4;

    private final GeotaggedScreenshot screenshot;
    private final Button closeButton;
    private final String missingMessage;
    private final Screen parent;

    public ScreenshotView(GeotaggedScreenshot screenshot, Vec3 coordinates, Screen parent) {
        super(Component.empty());
        this.screenshot = screenshot;
        this.missingMessage = "Couldn't find screenshot for: " + coordinates;
        this.parent = parent;
        this.closeButton = Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT).build();
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();
        closeButton.setX((width - BUTTON_WIDTH) / 2);
        closeButton.setY(height - BUTTON_HEIGHT - OFFSET);
        addRenderableWidget(closeButton);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        extractBackground(guiGraphics, mouseX, mouseY, partialTick);

        CompletableFuture<NativeImage> future = screenshot.getImageFuture(ImageType.ORIGINAL);
        if (future != null && future.isDone()) {
            NativeImage image = future.getNow(null);
            if (image != null) {
                float ratio = (float) image.getWidth() / image.getHeight();
                int imageHeight = height - OFFSET * 3 - BUTTON_HEIGHT;
                int imageWidth = (int) (imageHeight * ratio);
                if (imageWidth > width - OFFSET * 3) {
                    imageWidth = width - OFFSET * 3;
                    imageHeight = (int) (imageWidth / ratio);
                }

                int x = (width - imageWidth) / 2;
                int y = (height - imageHeight) / 2 - OFFSET * 3;
                RenderUtil.renderImage(guiGraphics, screenshot.getId(ImageType.ORIGINAL), image.getWidth(), image.getHeight(), imageWidth, imageHeight, x, y);
            } else {
                guiGraphics.centeredText(font, missingMessage, width / 2, height / 2, 0xFFFFFFFF);
            }
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        screenshot.close(ImageType.ORIGINAL);
        Minecraft.getInstance().setScreen(parent);
    }
}

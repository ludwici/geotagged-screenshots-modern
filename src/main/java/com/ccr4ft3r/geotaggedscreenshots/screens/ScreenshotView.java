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
    private final Screen parent;

    public ScreenshotView(GeotaggedScreenshot screenshot, Vec3 coordinates, Screen parent) {
        super(Component.empty());
        this.screenshot = screenshot;
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        CompletableFuture<NativeImage> future = screenshot.getImageFuture(ImageType.ORIGINAL);
        if (future == null) {
            renderStatus(graphics, "Screenshot file not found");
        } else if (!future.isDone()) {
            renderStatus(graphics, "Loading screenshot...");
        } else {
            NativeImage image = future.getNow(null);
            if (image == null) {
                renderStatus(graphics, "Could not read screenshot (see latest.log)");
            } else {
                float ratio = (float) image.getWidth() / image.getHeight();
                int imageHeight = height - OFFSET * 3 - BUTTON_HEIGHT;
                int imageWidth = (int) (imageHeight * ratio);
                if (imageWidth > width - OFFSET * 3) {
                    imageWidth = width - OFFSET * 3;
                    imageHeight = (int) (imageWidth / ratio);
                }

                int x = (width - imageWidth) / 2;
                int y = (height - imageHeight) / 2 - OFFSET * 3;
                var texture = screenshot.getId(ImageType.ORIGINAL);
                if (texture == null) {
                    renderStatus(graphics, "Preparing screenshot texture...");
                } else {
                    RenderUtil.renderImage(graphics, texture, image.getWidth(), image.getHeight(), imageWidth, imageHeight, x, y);
                }
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF141820);
    }

    private void renderStatus(GuiGraphicsExtractor graphics, String message) {
        graphics.centeredText(font, message, width / 2, height / 2 - 8, 0xFFFFFFFF);
        String name = screenshot.getFile(ImageType.ORIGINAL).getName();
        if (name.length() > 70) {
            name = name.substring(0, 67) + "...";
        }
        graphics.centeredText(font, name, width / 2, height / 2 + 8, 0xFFB0B0B0);
    }

    @Override
    public void onClose() {
        screenshot.close(ImageType.ORIGINAL);
        Minecraft.getInstance().setScreen(parent);
    }
}

package com.ccr4ft3r.geotaggedscreenshots.screens;

import com.ccr4ft3r.geotaggedscreenshots.container.GeotaggedScreenshot;
import com.ccr4ft3r.geotaggedscreenshots.container.ImageType;
import com.ccr4ft3r.geotaggedscreenshots.util.RenderUtil;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, width, height, 0xFF141820);

        CompletableFuture<NativeImage> future = screenshot.getImageFuture(ImageType.ORIGINAL);
        if (future == null) {
            renderStatus(guiGraphics, "Screenshot file not found", screenshot.getFile(ImageType.ORIGINAL).getName());
        } else if (!future.isDone()) {
            renderStatus(guiGraphics, "Loading screenshot...", screenshot.getFile(ImageType.ORIGINAL).getName());
        } else {
            NativeImage image = future.getNow(null);
            if (image == null) {
                renderStatus(guiGraphics, "Could not read screenshot (see latest.log)", screenshot.getFile(ImageType.ORIGINAL).getName());
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
                    renderStatus(guiGraphics, "Preparing screenshot texture...", screenshot.getFile(ImageType.ORIGINAL).getName());
                } else {
                    RenderUtil.renderImage(guiGraphics, texture, image.getWidth(), image.getHeight(), imageWidth, imageHeight, x, y);
                }
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderStatus(GuiGraphics graphics, String message, String filename) {
        graphics.drawCenteredString(font, message, width / 2, height / 2 - 7, 0xFFFFFF);
        graphics.drawCenteredString(font, font.plainSubstrByWidth(filename, Math.max(40, width - 32)), width / 2, height / 2 + 7, 0xB0B0B0);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        screenshot.close(ImageType.ORIGINAL);
        Minecraft.getInstance().setScreen(parent);
    }
}

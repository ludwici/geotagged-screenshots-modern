package com.ccr4ft3r.geotaggedscreenshots.util;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class RenderUtil {
    public static void renderImage(GuiGraphics graphics, ResourceLocation textureId, int width, int height, int scaledWidth, int scaledHeight, int x, int y) {
        if (textureId == null) {
            return;
        }

        graphics.blit(textureId, x, y, scaledWidth, scaledHeight, 0.0F, 0.0F, width, height, width, height);
    }
}

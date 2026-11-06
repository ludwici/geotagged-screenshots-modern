package com.ccr4ft3r.geotaggedscreenshots.util;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class RenderUtil {
    public static void renderImage(GuiGraphicsExtractor graphics, Identifier textureId, int width, int height, int scaledWidth, int scaledHeight, int x, int y) {
        if (textureId == null) {
            return;
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, textureId, x, y, 0.0F, 0.0F, scaledWidth, scaledHeight, width, height, width, height);
    }
}

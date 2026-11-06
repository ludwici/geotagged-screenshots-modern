package com.ccr4ft3r.geotaggedscreenshots.util;

public final class ThumbnailGeometry {
    private static final int MIN_WORLD_MAP_WIDTH = 24;
    private static final int MAX_WORLD_MAP_WIDTH = 144;
    private static final int MIN_WORLD_MAP_HEIGHT = 16;
    private static final int MINIMAP_WIDTH = 28;

    private ThumbnailGeometry() {
    }

    public static int worldMapWidth(int imageWidth, float scale) {
        return Math.max(MIN_WORLD_MAP_WIDTH, Math.min(MAX_WORLD_MAP_WIDTH, (int) (imageWidth * scale / 6.0F)));
    }

    public static int worldMapHeight(int imageWidth, int imageHeight, float scale) {
        return Math.max(MIN_WORLD_MAP_HEIGHT, (int) ((double) imageHeight * worldMapWidth(imageWidth, scale) / imageWidth));
    }

    public static int minimapWidth() {
        return MINIMAP_WIDTH;
    }

    public static int minimapHeight(int imageWidth, int imageHeight) {
        return Math.max(8, Math.round((float) imageHeight * MINIMAP_WIDTH / imageWidth));
    }
}

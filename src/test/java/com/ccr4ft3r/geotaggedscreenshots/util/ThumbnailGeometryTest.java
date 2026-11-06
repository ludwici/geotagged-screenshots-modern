package com.ccr4ft3r.geotaggedscreenshots.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThumbnailGeometryTest {
    @Test
    void scalesWorldMapThumbnailsWithOriginalAspectRatio() {
        assertEquals(66, ThumbnailGeometry.worldMapWidth(400, 1.0F));
        assertEquals(37, ThumbnailGeometry.worldMapHeight(400, 225, 1.0F));
        assertEquals(144, ThumbnailGeometry.worldMapWidth(400, 10.0F));
        assertEquals(81, ThumbnailGeometry.worldMapHeight(400, 225, 10.0F));
    }

    @Test
    void clampsSmallWorldMapThumbnails() {
        assertEquals(24, ThumbnailGeometry.worldMapWidth(400, 0.1F));
        assertEquals(16, ThumbnailGeometry.worldMapHeight(400, 225, 0.1F));
    }

    @Test
    void keepsMinimapThumbnailsCompact() {
        assertEquals(28, ThumbnailGeometry.minimapWidth());
        assertEquals(16, ThumbnailGeometry.minimapHeight(400, 225));
        assertEquals(28, ThumbnailGeometry.minimapHeight(400, 400));
    }
}

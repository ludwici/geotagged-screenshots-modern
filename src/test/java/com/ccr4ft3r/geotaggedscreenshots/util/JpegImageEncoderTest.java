package com.ccr4ft3r.geotaggedscreenshots.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpegImageEncoderTest {
    @TempDir
    Path tempDir;

    @Test
    void convertsAbgrToRgb() {
        assertEquals(0x112233, JpegImageEncoder.toRgb(0xFF332211));
    }

    @Test
    void writesActualJpegPixels() throws Exception {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, 0x1144DD);
            }
        }

        File target = tempDir.resolve("screenshot.jpg").toFile();
        JpegImageEncoder.write(image, target);
        byte[] bytes = java.nio.file.Files.readAllBytes(target.toPath());
        assertTrue(bytes.length > 2);
        assertEquals(0xFF, bytes[0] & 0xFF);
        assertEquals(0xD8, bytes[1] & 0xFF);

        BufferedImage decoded = ImageIO.read(target);
        assertNotNull(decoded);
        assertEquals(16, decoded.getWidth());
        assertEquals(16, decoded.getHeight());
        int pixel = decoded.getRGB(4, 4);
        assertTrue(((pixel >> 16) & 0xFF) < 70);
        assertTrue((pixel & 0xFF) > 170);
    }
}

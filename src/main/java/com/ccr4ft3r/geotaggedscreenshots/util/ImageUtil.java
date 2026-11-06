package com.ccr4ft3r.geotaggedscreenshots.util;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.Util;

import java.awt.image.BufferedImage;
import java.util.Locale;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

import static com.ccr4ft3r.geotaggedscreenshots.ModConstants.THUMBNAIL_DIR;

public class ImageUtil {
    public static File saveScreenshot(NativeImage image, File requestedFile, boolean jpeg) throws IOException {
        File output = getUniqueFile(requestedFile, jpeg ? ".jpg" : ".png");
        Files.createDirectories(output.toPath().getParent());

        if (jpeg) {
            BufferedImage bufferedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    bufferedImage.setRGB(x, y, JpegImageEncoder.toRgb(image.getPixelRGBA(x, y)));
                }
            }

            JpegImageEncoder.write(bufferedImage, output);
        } else {
            image.writeToFile(output);
        }

        return output;
    }

    private static File getUniqueFile(File requestedFile, String extension) {
        String name = requestedFile.getName().replaceFirst("\\.[^.]+$", "");
        File parent = requestedFile.getParentFile();
        String otherExtension = ".jpg".equals(extension) ? ".png" : ".jpg";
        String candidate = name;
        int index = 1;

        while (new File(parent, candidate + extension).exists() || new File(parent, candidate + otherExtension).exists()) {
            candidate = name + "_" + index++;
        }

        return new File(parent, candidate + extension);
    }

    public static File createThumbnail(NativeImage original, File target) {
        int width = 400;
        int height = Math.max(1, (int) (width * ((float) original.getHeight() / original.getWidth())));
        String filename = target.getName().replaceFirst("\\.[^.]+$", "") + ".png";
        File thumbnailFile = new File(THUMBNAIL_DIR, filename);

        try (NativeImage thumbnail = new NativeImage(width, height, false)) {
            original.resizeSubRectTo(0, 0, original.getWidth(), original.getHeight(), thumbnail);
            if (thumbnailFile.getParentFile().exists() || thumbnailFile.getParentFile().mkdirs()) {
                thumbnail.writeToFile(thumbnailFile);
            }
        } catch (IOException e) {
            LogUtils.getLogger().error("Couldn't write screenshot thumbnail {}", thumbnailFile, e);
        }

        return thumbnailFile;
    }

    public static CompletableFuture<NativeImage> loadImage(File file) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String filename = file.getName().toLowerCase(Locale.ROOT);
                if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                    BufferedImage buffered = JpegImageEncoder.read(file);
                    NativeImage image = new NativeImage(buffered.getWidth(), buffered.getHeight(), false);
                    try {
                        for (int y = 0; y < buffered.getHeight(); y++) {
                            for (int x = 0; x < buffered.getWidth(); x++) {
                                image.setPixelRGBA(x, y, JpegImageEncoder.toAbgr(buffered.getRGB(x, y)));
                            }
                        }

                        return image;
                    } catch (Exception e) {
                        image.close();
                        throw e;
                    }
                }

                try (InputStream stream = new FileInputStream(file)) {
                    return NativeImage.read(stream);
                }
            } catch (Exception e) {
                LogUtils.getLogger().error("Couldn't load screenshot from {}", file.getAbsolutePath(), e);
                return null;
            }
        }, Util.backgroundExecutor());
    }
}

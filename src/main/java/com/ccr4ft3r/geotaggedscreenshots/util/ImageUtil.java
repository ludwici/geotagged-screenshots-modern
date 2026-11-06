package com.ccr4ft3r.geotaggedscreenshots.util;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.Util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

import static com.ccr4ft3r.geotaggedscreenshots.ModConstants.THUMBNAIL_DIR;

public class ImageUtil {
    public static File saveScreenshot(NativeImage image, File requestedFile, boolean jpeg) throws IOException {
        File output = jpeg ? getUniqueJpegFile(requestedFile) : requestedFile;
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

    private static File getUniqueJpegFile(File requestedFile) {
        String name = requestedFile.getName().replaceFirst("\\.[^.]+$", "");
        File parent = requestedFile.getParentFile();
        File target = new File(parent, name + ".jpg");
        int index = 1;

        while (target.exists()) {
            target = new File(parent, name + "_" + index++ + ".jpg");
        }

        return target;
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
            try (InputStream stream = new FileInputStream(file)) {
                return NativeImage.read(stream);
            } catch (Exception e) {
                LogUtils.getLogger().error("Couldn't load screenshot {}", file, e);
            }
            return null;
        }, Util.backgroundExecutor());
    }
}

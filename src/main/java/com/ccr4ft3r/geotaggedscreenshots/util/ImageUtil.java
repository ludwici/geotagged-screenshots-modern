package com.ccr4ft3r.geotaggedscreenshots.util;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

import static com.ccr4ft3r.geotaggedscreenshots.ModConstants.THUMBNAIL_DIR;

public class ImageUtil {
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

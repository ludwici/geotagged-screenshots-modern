package com.ccr4ft3r.geotaggedscreenshots.container;

import com.ccr4ft3r.geotaggedscreenshots.util.ImageUtil;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import java.util.UUID;

import java.io.File;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class GeotaggedScreenshot {
    private final String name;
    private final Map<ImageType, File> fileByType = new EnumMap<>(ImageType.class);
    private final Map<ImageType, CompletableFuture<NativeImage>> futureByType = new EnumMap<>(ImageType.class);
    private final Map<ImageType, Identifier> textureByType = new EnumMap<>(ImageType.class);

    public GeotaggedScreenshot(String name) {
        this.name = name;
    }

    public CompletableFuture<NativeImage> getImageFuture(ImageType type) {
        if (!futureByType.containsKey(type)) {
            File file = getFile(type);
            if (file.exists()) {
                futureByType.put(type, ImageUtil.loadImage(file));
            }
        }
        return futureByType.get(type);
    }

    public GeotaggedScreenshot setFile(ImageType type, File file) {
        fileByType.put(type, file);
        return this;
    }

    public File getFile(ImageType type) {
        return fileByType.computeIfAbsent(type, t -> {
            File png = new File(type.getDir(), name + ".png");
            if (png.exists() || type == ImageType.THUMBNAIL) {
                return png;
            }

            File jpg = new File(type.getDir(), name + ".jpg");
            return jpg.exists() ? jpg : png;
        });
    }

    public Identifier getId(ImageType type) {
        Identifier id = textureByType.get(type);
        if (id != null) {
            return id;
        }

        CompletableFuture<NativeImage> future = getImageFuture(type);
        if (future == null || !future.isDone()) {
            return null;
        }

        NativeImage image = future.getNow(null);
        if (image == null) {
            return null;
        }

        id = Identifier.fromNamespaceAndPath("geotagged_screenshots", "images/" + UUID.randomUUID().toString());
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "Geotagged " + type.name().toLowerCase(), image));
        textureByType.put(type, id);
        return id;
    }

    public void close() {
        Arrays.stream(ImageType.values()).forEach(this::close);
    }

    public void close(ImageType type) {
        Identifier id = textureByType.remove(type);
        if (id != null) {
            Minecraft.getInstance().getTextureManager().release(id);
        }

        CompletableFuture<NativeImage> future = futureByType.remove(type);
        if (future != null && id == null) {
            NativeImage image = future.getNow(null);
            if (image != null) {
                image.close();
            } else {
                future.cancel(true);
            }
        }
    }

    public String getName() {
        return name;
    }
}

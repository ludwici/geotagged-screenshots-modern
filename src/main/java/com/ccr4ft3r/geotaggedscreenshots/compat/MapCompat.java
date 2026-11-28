package com.ccr4ft3r.geotaggedscreenshots.compat;

import net.fabricmc.loader.api.FabricLoader;

public final class MapCompat {
    private MapCompat() {
    }

    public static boolean hasXaero() {
        return isLoaded("xaeroworldmap") && isLoaded("xaerominimap");
    }

    private static boolean isLoaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }
}

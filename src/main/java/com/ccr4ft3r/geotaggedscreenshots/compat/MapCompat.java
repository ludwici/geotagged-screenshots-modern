package com.ccr4ft3r.geotaggedscreenshots.compat;

import net.neoforged.fml.ModList;

public final class MapCompat {
    private MapCompat() {
    }

    public static boolean hasXaero() {
        return isLoaded("xaeroworldmap") && isLoaded("xaerominimap");
    }

    private static boolean isLoaded(String id) {
        return ModList.get().isLoaded(id);
    }
}

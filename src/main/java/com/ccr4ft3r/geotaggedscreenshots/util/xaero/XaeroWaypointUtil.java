package com.ccr4ft3r.geotaggedscreenshots.util.xaero;

import com.ccr4ft3r.geotaggedscreenshots.GeotaggedScreenshots;
import com.ccr4ft3r.geotaggedscreenshots.container.GeotaggedScreenshot;
import com.ccr4ft3r.geotaggedscreenshots.container.ImageType;
import com.ccr4ft3r.geotaggedscreenshots.container.ScreenshotMetadata;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.map.mods.SupportMods;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static com.ccr4ft3r.geotaggedscreenshots.ModConstants.getWaypointSet;

public final class XaeroWaypointUtil {
    private static WaypointSet synchronizedWaypointSet;
    private static Object synchronizedAlbum;
    private static Object synchronizedLevel;
    private static int synchronizedWaypointCount = -1;
    private static int scanTicks;

    private XaeroWaypointUtil() {
    }

    private static MinimapSession getSession() {
        return BuiltInHudModules.MINIMAP.getCurrentSession();
    }

    public static WaypointSet getScreenshotWaypointSet() {
        if (SupportMods.xaeroMinimap == null) {
            return null;
        }

        MinimapSession session = getSession();
        if (session == null) {
            return null;
        }

        MinimapWorld world = session.getWorldManager().getCurrentWorld();
        if (world == null) {
            return null;
        }

        String setName = getWaypointSet();
        WaypointSet set = world.getWaypointSet(setName);
        if (set == null) {
            world.addWaypointSet(setName);
            set = world.getWaypointSet(setName);
        }

        return set;
    }

    public static boolean needsSync() {
        WaypointSet set = getScreenshotWaypointSet();
        if (set == null) {
            synchronizedWaypointSet = null;
            return false;
        }

        if (set != synchronizedWaypointSet || synchronizedAlbum != GeotaggedScreenshots.ALBUM
                || synchronizedLevel != Minecraft.getInstance().level) {
            return true;
        }

        if (++scanTicks < 20) {
            return false;
        }

        scanTicks = 0;
        return countWaypoints(set) != synchronizedWaypointCount;
    }

    private static int countWaypoints(WaypointSet set) {
        int count = 0;
        for (Waypoint waypoint : set.getWaypoints()) {
            count++;
        }
        return count;
    }

    public static void reset() {
        synchronizedWaypointSet = null;
        synchronizedAlbum = null;
        synchronizedLevel = null;
        synchronizedWaypointCount = -1;
        scanTicks = 0;
    }

    public static void requestWorldMapRefresh() {
        if (SupportMods.xaeroMinimap != null) {
            SupportMods.xaeroMinimap.requestWaypointsRefresh();
        }
    }

    public static boolean loadExistingWaypoints() {
        WaypointSet set = getScreenshotWaypointSet();
        Minecraft minecraft = Minecraft.getInstance();
        if (set == null || minecraft.level == null || GeotaggedScreenshots.ALBUM == null) {
            return false;
        }

        Set<String> names = new HashSet<>();
        for (Waypoint waypoint : set.getWaypoints()) {
            String name = waypoint.getName();
            names.add(name);
            if (GeotaggedScreenshots.ALBUM.getScreenshot(name) == null) {
                GeotaggedScreenshot screenshot = new GeotaggedScreenshot(name);
                if (screenshot.getFile(ImageType.ORIGINAL).isFile()) {
                    GeotaggedScreenshots.ALBUM.add(screenshot);
                    screenshot.getImageFuture(ImageType.THUMBNAIL);
                    ScreenshotMetadata.importWaypoint(name, new Vec3(waypoint.getX(), waypoint.getY(), waypoint.getZ()), minecraft.level.dimension());
                }
            }
        }

        boolean changed = false;
        String dimension = minecraft.level.dimension().identifier().toString();
        for (ScreenshotMetadata.Entry entry : ScreenshotMetadata.entries()) {
            if (dimension.equals(entry.dimension()) && !names.contains(entry.name())
                    && GeotaggedScreenshots.ALBUM.getScreenshot(entry.name()) != null) {
                set.add(new Waypoint(entry.x(), entry.y(), entry.z(), entry.name(), "S", WaypointColor.AQUA));
                names.add(entry.name());
                changed = true;
            }
        }

        if (changed) {
            saveCurrentWorld();
        } else {
            requestWorldMapRefresh();
        }

        synchronizedWaypointSet = set;
        synchronizedAlbum = GeotaggedScreenshots.ALBUM;
        synchronizedLevel = minecraft.level;
        synchronizedWaypointCount = countWaypoints(set);
        scanTicks = 0;
        return true;
    }

    private static void saveCurrentWorld() {
        MinimapSession session = getSession();
        if (session == null) {
            return;
        }

        MinimapWorld world = session.getWorldManager().getCurrentWorld();
        if (world == null) {
            return;
        }

        try {
            session.getWorldManagerIO().saveWorld(world);
        } catch (IOException exception) {
            LogUtils.getLogger().error("Could not save Xaero screenshot waypoints", exception);
        }

        if (SupportMods.xaeroMinimap != null) {
            SupportMods.xaeroMinimap.requestWaypointsRefresh();
        }
    }
}

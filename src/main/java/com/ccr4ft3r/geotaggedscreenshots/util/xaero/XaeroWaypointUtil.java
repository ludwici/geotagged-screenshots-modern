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

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static com.ccr4ft3r.geotaggedscreenshots.ModConstants.getWaypointSet;

public final class XaeroWaypointUtil {
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
                    ScreenshotMetadata.importWaypoint(name, new Vec3(waypoint.getX(), waypoint.getY(), waypoint.getZ()), minecraft.level.dimension());
                }
            }
        }

        boolean changed = false;
        String dimension = minecraft.level.dimension().location().toString();
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
        }

        return true;
    }

    public static void addNewScreenshotWaypoint(Vec3 position, File screenshotFile, File thumbnailFile) {
        WaypointSet set = getScreenshotWaypointSet();
        if (set == null || GeotaggedScreenshots.ALBUM == null) {
            return;
        }

        String name = screenshotFile.getName().replaceFirst("\\.[^.]+$", "");
        Waypoint waypoint = new Waypoint((int) position.x, (int) position.y, (int) position.z, name, "S", WaypointColor.AQUA);
        set.add(waypoint);
        saveCurrentWorld();
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

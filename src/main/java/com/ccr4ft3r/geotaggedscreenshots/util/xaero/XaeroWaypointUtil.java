package com.ccr4ft3r.geotaggedscreenshots.util.xaero;

import com.ccr4ft3r.geotaggedscreenshots.GeotaggedScreenshots;
import com.ccr4ft3r.geotaggedscreenshots.container.GeotaggedScreenshot;
import com.ccr4ft3r.geotaggedscreenshots.container.ImageType;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.map.mods.SupportMods;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;

import java.io.File;

import static com.ccr4ft3r.geotaggedscreenshots.ModConstants.getWaypointSet;

public class XaeroWaypointUtil {
    public static WaypointSet getScreenshotWaypointSet() {
        if (SupportMods.xaeroMinimap == null) {
            return null;
        }

        MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
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
            requestWorldMapRefresh();
        }

        return set;
    }

    public static void requestWorldMapRefresh() {
        if (SupportMods.xaeroMinimap != null) {
            SupportMods.xaeroMinimap.requestWaypointsRefresh();
        }
    }

    public static void addNewScreenshotWaypoint(Vec3 position, File screenshotFile, File thumbnailFile) {
        WaypointSet set = getScreenshotWaypointSet();
        if (set == null || GeotaggedScreenshots.ALBUM == null) {
            return;
        }

        String name = screenshotFile.getName().replaceFirst("\\.[^.]+$", "");
        Waypoint waypoint = new Waypoint((int) position.x, (int) position.y, (int) position.z, name, "S", WaypointColor.AQUA);
        set.add(waypoint);
        requestWorldMapRefresh();

        GeotaggedScreenshot screenshot = new GeotaggedScreenshot(name)
                .setFile(ImageType.ORIGINAL, screenshotFile)
                .setFile(ImageType.THUMBNAIL, thumbnailFile);
        GeotaggedScreenshots.ALBUM.add(screenshot);
        screenshot.getImageFuture(ImageType.THUMBNAIL);

        LogUtils.getLogger().info("Added geotagged screenshot waypoint {} at {} (world map refresh requested)", name, position);
    }
}

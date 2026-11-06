package com.ccr4ft3r.geotaggedscreenshots.util.xaero;

import com.ccr4ft3r.geotaggedscreenshots.GeotaggedScreenshots;
import com.ccr4ft3r.geotaggedscreenshots.container.GeotaggedScreenshot;
import com.ccr4ft3r.geotaggedscreenshots.container.ImageType;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.map.MapProcessor;
import xaero.map.WorldMapSession;
import xaero.map.core.XaeroWorldMapCore;
import xaero.map.gui.GuiMap;
import xaero.map.mods.SupportMods;

import java.io.File;

import static com.ccr4ft3r.geotaggedscreenshots.ModConstants.getWaypointSet;

public class XaeroWaypointUtil {
    public static void initWaypoints() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || XaeroWorldMapCore.currentSession == null || SupportMods.xaeroMinimap == null) {
            return;
        }

        WorldMapSession session = XaeroWorldMapCore.currentSession;
        MapProcessor processor = session.getMapProcessor();
        if (processor == null || processor.getMapWorld() == null) {
            return;
        }

        ResourceKey<Level> dimension = minecraft.level.dimension();
        GuiMap guiMap = new GuiMap(null, null, processor, minecraft.player);
        SupportMods.xaeroMinimap.checkWaypoints(processor.getMapWorld().isMultiplayer(), dimension, "", guiMap.width, guiMap.height, guiMap, processor.getMapWorld(), processor.getWorldDimensionTypeRegistry());
    }

    public static WaypointSet getScreenshotWaypointSet() {
        if (SupportMods.xaeroMinimap == null) {
            return null;
        }

        MinimapWorld world = SupportMods.xaeroMinimap.getWaypointWorld();
        if (world == null) {
            initWaypoints();
            world = SupportMods.xaeroMinimap.getWaypointWorld();
        }

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

    public static void addNewScreenshotWaypoint(Vec3 position, File screenshotFile, File thumbnailFile) {
        WaypointSet set = getScreenshotWaypointSet();
        if (set == null || GeotaggedScreenshots.ALBUM == null) {
            return;
        }

        String name = screenshotFile.getName().replaceFirst("\\.[^.]+$", "");
        Waypoint waypoint = new Waypoint((int) position.x, (int) position.y, (int) position.z, name, "S", WaypointColor.AQUA);
        set.add(waypoint);

        GeotaggedScreenshot screenshot = new GeotaggedScreenshot(name)
                .setFile(ImageType.ORIGINAL, screenshotFile)
                .setFile(ImageType.THUMBNAIL, thumbnailFile);
        GeotaggedScreenshots.ALBUM.add(screenshot);
        screenshot.getImageFuture(ImageType.THUMBNAIL);

        LogUtils.getLogger().debug("Added screenshot waypoint at {}", position);
    }
}

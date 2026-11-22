package com.ccr4ft3r.geotaggedscreenshots.util.xaero;

import com.ccr4ft3r.geotaggedscreenshots.GeotaggedScreenshots;
import com.ccr4ft3r.geotaggedscreenshots.container.GeotaggedScreenshot;
import com.ccr4ft3r.geotaggedscreenshots.container.ImageType;
import com.ccr4ft3r.geotaggedscreenshots.container.ScreenshotMetadata;
import com.ccr4ft3r.geotaggedscreenshots.mixin.xaero.SupportXaeroMinimapAccessor;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.common.minimap.waypoints.WaypointWorld;
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

        SupportXaeroMinimapAccessor minimap = (SupportXaeroMinimapAccessor) SupportMods.xaeroMinimap;
        if (minimap.waypointWorld() == null) {
            initWaypoints();
        }

        WaypointWorld world = minimap.waypointWorld();
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

    public static void loadExistingWaypoints() {
        WaypointSet set = getScreenshotWaypointSet();
        Minecraft minecraft = Minecraft.getInstance();
        if (set == null || minecraft.level == null || GeotaggedScreenshots.ALBUM == null) {
            return;
        }

        java.util.Set<String> names = new java.util.HashSet<>();
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

        String dimension = minecraft.level.dimension().location().toString();
        for (ScreenshotMetadata.Entry entry : ScreenshotMetadata.entries()) {
            if (dimension.equals(entry.dimension()) && !names.contains(entry.name())
                    && GeotaggedScreenshots.ALBUM.getScreenshot(entry.name()) != null) {
                set.add(new Waypoint(entry.x(), entry.y(), entry.z(), entry.name(), "S", WaypointColor.AQUA));
                names.add(entry.name());
            }
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

    }
}

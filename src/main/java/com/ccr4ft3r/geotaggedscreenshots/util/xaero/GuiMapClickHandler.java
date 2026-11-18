package com.ccr4ft3r.geotaggedscreenshots.util.xaero;

import com.ccr4ft3r.geotaggedscreenshots.GeotaggedScreenshots;
import com.ccr4ft3r.geotaggedscreenshots.container.GeotaggedScreenshot;
import com.ccr4ft3r.geotaggedscreenshots.screens.ScreenshotView;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ScreenEvent;
import xaero.map.element.HoveredMapElementHolder;
import xaero.map.gui.GuiMap;
import xaero.map.mods.gui.Waypoint;

import java.lang.reflect.Field;

public final class GuiMapClickHandler {
    private static final int LEFT_MOUSE_BUTTON = 1;
    private static final int MAX_DRAG_DISTANCE = 5;
    private static Field viewedField;
    private static boolean lookupFailed;
    private static GuiMap pressedMap;
    private static double pressedX;
    private static double pressedY;

    private GuiMapClickHandler() {
    }

    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != LEFT_MOUSE_BUTTON || !(event.getScreen() instanceof GuiMap map)) {
            return;
        }

        pressedMap = map;
        pressedX = event.getMouseX();
        pressedY = event.getMouseY();
    }

    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Post event) {
        if (event.getButton() != LEFT_MOUSE_BUTTON) {
            return;
        }

        GuiMap initialScreen = pressedMap;
        pressedMap = null;
        if (initialScreen == null || event.getScreen() != initialScreen
                || Math.abs(event.getMouseX() - pressedX) >= MAX_DRAG_DISTANCE
                || Math.abs(event.getMouseY() - pressedY) >= MAX_DRAG_DISTANCE
                || GeotaggedScreenshots.ALBUM == null) {
            return;
        }

        HoveredMapElementHolder<?, ?> hovered = getHoveredWaypoint(initialScreen);
        if (hovered == null || !(hovered.getElement() instanceof Waypoint waypoint)) {
            return;
        }

        GeotaggedScreenshot screenshot = GeotaggedScreenshots.ALBUM.getScreenshot(waypoint.getName());
        if (screenshot == null) {
            return;
        }

        Minecraft.getInstance().gui.setScreen(new ScreenshotView(screenshot, new Vec3(waypoint.getX(), waypoint.getY(), waypoint.getZ()), initialScreen));
    }

    private static HoveredMapElementHolder<?, ?> getHoveredWaypoint(GuiMap map) {
        if (lookupFailed) {
            return null;
        }

        try {
            if (viewedField == null) {
                viewedField = GuiMap.class.getDeclaredField("viewed");
                viewedField.setAccessible(true);
            }

            Object hovered = viewedField.get(map);
            if (hovered instanceof HoveredMapElementHolder<?, ?> holder) {
                return holder;
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            lookupFailed = true;
            LogUtils.getLogger().warn("Couldn't access Xaero's World Map hovered waypoint", e);
        }

        return null;
    }
}

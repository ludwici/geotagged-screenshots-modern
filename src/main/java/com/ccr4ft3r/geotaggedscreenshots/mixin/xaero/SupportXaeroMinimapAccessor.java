package com.ccr4ft3r.geotaggedscreenshots.mixin.xaero;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.common.minimap.waypoints.WaypointWorld;
import xaero.map.mods.SupportXaeroMinimap;

@Mixin(SupportXaeroMinimap.class)
public interface SupportXaeroMinimapAccessor {
    @Accessor("waypointWorld")
    WaypointWorld waypointWorld();
}

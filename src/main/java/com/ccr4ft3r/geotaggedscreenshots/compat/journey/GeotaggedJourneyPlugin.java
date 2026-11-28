package com.ccr4ft3r.geotaggedscreenshots.compat.journey;

import com.ccr4ft3r.geotaggedscreenshots.GeotaggedScreenshots;
import com.ccr4ft3r.geotaggedscreenshots.ModConstants;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.event.MappingEvent;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.common.event.ClientEventRegistry;
import net.minecraft.client.Minecraft;

@JourneyMapPlugin(apiVersion = "2.0.0")
public class GeotaggedJourneyPlugin implements IClientPlugin {
    @Override
    public void initialize(IClientAPI api) {
        JourneyScreenshotMarkers.initialize(api);
        GeotaggedScreenshots.setJourneyRefresh(JourneyScreenshotMarkers::refresh);
        ClientEventRegistry.MAPPING_EVENT.subscribe(ModConstants.MODID, event -> {
            if (event.getStage() == MappingEvent.Stage.MAPPING_STARTED) {
                Minecraft.getInstance().execute(() -> {
                    JourneyScreenshotMarkers.beginMapping();
                    GeotaggedScreenshots.load();
                    JourneyScreenshotMarkers.refresh();
                });
            } else if (event.getStage() == MappingEvent.Stage.MAPPING_STOPPED) {
                Minecraft.getInstance().execute(JourneyScreenshotMarkers::clear);
            }
        });
    }

    @Override
    public String getModId() {
        return ModConstants.MODID;
    }
}

package com.xtoxray.client;

import com.xtoxray.XtoXray;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;

@EventBusSubscriber(modid=XtoXray.MOD_ID,value=net.neoforged.api.distmarker.Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class ClientModEvents {
    private ClientModEvents(){}

    @SubscribeEvent public static void registerKeyMappings(RegisterKeyMappingsEvent e){
        e.register(XrayClient.TOGGLE_KEY.get());
        e.register(XrayClient.VEIN_MINER_KEY.get());
        e.register(XrayClient.OPEN_MENU_KEY.get());
    }

    @SubscribeEvent public static void registerRenderBuffers(RegisterRenderBuffersEvent e){
        e.registerRenderBuffer(ColorXrayRenderer.COLOR_OUTLINE);
        e.registerRenderBuffer(ColorXrayRenderer.COLOR_FILL);
    }
}
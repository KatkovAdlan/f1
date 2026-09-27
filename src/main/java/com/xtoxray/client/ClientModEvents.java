package com.xtoxray.client;

import com.xtoxray.XtoXray;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(
    modid = XtoXray.MOD_ID,
    value = net.neoforged.api.distmarker.Dist.CLIENT,
    bus = EventBusSubscriber.Bus.MOD
)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(XrayClient.CONFIG_KEY.get());
        event.register(XrayClient.TOGGLE_KEY.get());
        event.register(XrayClient.VEIN_MINER_KEY.get());
    }
}

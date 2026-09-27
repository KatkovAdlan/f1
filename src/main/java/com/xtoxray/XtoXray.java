package com.xtoxray;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(XtoXray.MOD_ID)
public final class XtoXray {
    public static final String MOD_ID = "xtoxray";

    public XtoXray(IEventBus modBus) {
        XrayState.getInstance().load();
        XrayVeinMiner.register();
    }
}

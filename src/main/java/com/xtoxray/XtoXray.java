package com.xtoxray;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(XtoXray.MOD_ID)
public final class XtoXray {
    public static final String MOD_ID = "xtoxray";
    private static String modVersion = "unknown";

    public XtoXray(IEventBus modBus, ModContainer modContainer) {
        modVersion = modContainer.getModInfo().getVersion().toString();
        XrayState.getInstance().load();
        XrayVeinMiner.register();
    }

    public static String getModVersion() {
        return modVersion;
    }
}

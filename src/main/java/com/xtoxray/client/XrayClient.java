package com.xtoxray.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.xtoxray.XrayState;
import com.xtoxray.XtoXray;
import com.xtoxray.client.gui.XrayConfigScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = XtoXray.MOD_ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class XrayClient {
    public static final Lazy<KeyMapping> CONFIG_KEY = Lazy.of(() -> new KeyMapping(
        "key.xtoxray.open_config",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_O,
        "key.categories.xtoxray"
    ));

    public static final Lazy<KeyMapping> TOGGLE_KEY = Lazy.of(() -> new KeyMapping(
        "key.xtoxray.toggle",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_X,
        "key.categories.xtoxray"
    ));

    public static final Lazy<KeyMapping> VEIN_MINER_KEY = Lazy.of(() -> new KeyMapping(
        "key.xtoxray.vein_miner",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_V,
        "key.categories.xtoxray"
    ));

    private static long lastX = Long.MIN_VALUE;
    private static int lastY = Integer.MIN_VALUE;
    private static long lastZ = Long.MIN_VALUE;

    private XrayClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        while (CONFIG_KEY.get().consumeClick()) {
            if (mc.player != null && mc.level != null && mc.screen == null) {
                mc.setScreen(new XrayConfigScreen(null));
            }
        }

        while (TOGGLE_KEY.get().consumeClick()) {
            if (mc.level != null && mc.player != null && mc.screen == null) {
                toggleXray(mc);
            }
        }

        while (VEIN_MINER_KEY.get().consumeClick()) {
            if (mc.level != null && mc.player != null && mc.screen == null) {
                toggleVeinMiner(mc);
            }
        }

        if (XrayState.getInstance().isActive() && mc.level != null && mc.player != null) {
            long x = mc.player.blockPosition().getX() >> 4;
            int y = mc.player.blockPosition().getY() >> 4;
            long z = mc.player.blockPosition().getZ() >> 4;

            if (x != lastX || y != lastY || z != lastZ) {
                lastX = x;
                lastY = y;
                lastZ = z;
                rebuildAll(mc);
            }
        }
    }

    public static void toggleXrayFromGui(Minecraft mc) {
        if (mc.player != null && mc.level != null) {
            toggleXray(mc);
        }
    }

    private static void toggleXray(Minecraft mc) {
        XrayState state = XrayState.getInstance();
        state.toggle();

        LocalPlayer player = mc.player;
        if (state.isActive()) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.NIGHT_VISION,
                -1,
                0,
                false,
                false,
                false
            ));
            player.displayClientMessage(
                Component.translatable("message.xtoxray.xray_on"),
                true
            );
        } else {
            player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
            player.displayClientMessage(
                Component.translatable("message.xtoxray.xray_off"),
                true
            );
        }

        lastX = Long.MIN_VALUE;
        lastY = Integer.MIN_VALUE;
        lastZ = Long.MIN_VALUE;
        rebuildAll(mc);
    }

    private static void toggleVeinMiner(Minecraft mc) {
        XrayState state = XrayState.getInstance();
        state.setVeinMiner(!state.isVeinMiner());

        mc.player.displayClientMessage(
            Component.translatable(
                state.isVeinMiner()
                    ? "message.xtoxray.vein_on"
                    : "message.xtoxray.vein_off"
            ),
            true
        );
    }

    public static void rebuildAll(Minecraft mc) {
        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }
}

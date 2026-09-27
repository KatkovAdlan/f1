package com.xtoxray.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.xtoxray.XrayState;
import com.xtoxray.XtoXray;
import com.xtoxray.client.gui.XrayConfigScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = XtoXray.MOD_ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class XrayClient {
    public static final Lazy<KeyMapping> TOGGLE_KEY = Lazy.of(() -> new KeyMapping(
        "key.xtoxray.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, "key.categories.xtoxray"
    ));

    public static final Lazy<KeyMapping> VEIN_MINER_KEY = Lazy.of(() -> new KeyMapping(
        "key.xtoxray.vein_miner", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.xtoxray"
    ));

    private static long lastX = Long.MIN_VALUE;
    private static int lastY = Integer.MIN_VALUE;
    private static long lastZ = Long.MIN_VALUE;

    private XrayClient() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        while (TOGGLE_KEY.get().consumeClick()) {
            if (mc.level != null && mc.player != null && mc.screen == null) toggleXray(mc);
        }

        while (VEIN_MINER_KEY.get().consumeClick()) {
            if (mc.level != null && mc.player != null && mc.screen == null) toggleVeinMiner(mc);
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
        if (mc.player != null && mc.level != null) toggleXray(mc);
    }

    private static void toggleXray(Minecraft mc) {
        XrayState state = XrayState.getInstance();
        state.toggle();
        LocalPlayer player = mc.player;

        if (state.isActive()) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.NIGHT_VISION, -1, 0, false, false, false
            ));
            player.displayClientMessage(Component.literal("X-Ray включён"), true);
        } else {
            player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
            player.displayClientMessage(Component.literal("X-Ray выключен"), true);
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
            Component.literal(state.isVeinMiner() ? "VeinMiner включён" : "VeinMiner выключен"), true
        );
    }

    public static void rebuildAll(Minecraft mc) {
        if (mc.levelRenderer != null) mc.levelRenderer.allChanged();
    }

    @SubscribeEvent
    public static void addPauseButton(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen screen)) return;

        int x = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2 - 100;
        int width = 200;
        int y = Minecraft.getInstance().getWindow().getGuiScaledHeight() / 4 + 120;

        for (var child : screen.children()) {
            if (child instanceof Button button &&
                button.getMessage().equals(Component.translatable("fml.menu.mods"))) {
                x = button.getX();
                width = button.getWidth();
                y = button.getY() + button.getHeight() + 4;
                break;
            }
        }

        final int finalX = x;
        final int finalY = y;
        final int finalWidth = width;

        event.addListener(Button.builder(
            Component.literal("X to Xray"),
            b -> Minecraft.getInstance().setScreen(new XrayConfigScreen(screen))
        ).bounds(finalX, finalY, finalWidth, 20).build());
    }
}

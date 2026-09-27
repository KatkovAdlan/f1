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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;

@EventBusSubscriber(modid = XtoXray.MOD_ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class XrayClient {
    public static final Lazy<KeyMapping> TOGGLE_KEY = Lazy.of(() -> new KeyMapping(
        "key.xtoxray.toggle",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_X,
        "key.categories.misc"
    ));

    private static long lastX = Long.MIN_VALUE;
    private static int lastY = Integer.MIN_VALUE;
    private static long lastZ = Long.MIN_VALUE;

    private XrayClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (TOGGLE_KEY.get().consumeClick()) {
            if (mc.level == null || mc.player == null) {
                continue;
            }
            toggleXray(mc);
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

    private static void toggleXray(Minecraft mc) {
        XrayState state = XrayState.getInstance();
        state.toggle();
        LocalPlayer player = mc.player;
        if (state.isActive()) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.NIGHT_VISION, -1, 0, false, false, false
            ));
        } else {
            player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
        }
        lastX = Long.MIN_VALUE;
        lastY = Integer.MIN_VALUE;
        lastZ = Long.MIN_VALUE;
        rebuildAll(mc);
    }

    public static void rebuildAll(Minecraft mc) {
        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }

    @SubscribeEvent
    public static void addPauseButton(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen screen)) {
            return;
        }
        int guiWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int guiHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int x = guiWidth / 2 - 102;
        int y = guiHeight / 4 + 96;
        event.addListener(Button.builder(Component.literal("X To Xray"), b ->
            Minecraft.getInstance().setScreen(new XrayConfigScreen(screen))
        ).bounds(x, y, 204, 20).build());
    }

    public static void loadCustomPack() {
        Minecraft mc = Minecraft.getInstance();
        String configured = XrayState.getInstance().getCustomPackPath();
        if (configured == null || configured.isBlank()) {
            return;
        }

        Path source = Path.of(configured);
        Path target = mc.gameDirectory.toPath().resolve("xtoxray_custom.zip");
        try {
            if (!Files.exists(source) || !configured.toLowerCase().endsWith(".zip")) {
                return;
            }
            Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            var repository = mc.getResourcePackRepository();
            repository.reload();
            Collection<String> selected = repository.getSelectedIds();
            LinkedHashSet<String> ids = new LinkedHashSet<>(selected);
            ids.add("file/xtoxray_custom");
            repository.setSelected(ids);
            mc.reloadResourcePacks();
        } catch (IOException ignored) {
        }
    }

    public static void unloadCustomPack() {
        Minecraft mc = Minecraft.getInstance();
        try {
            var repository = mc.getResourcePackRepository();
            repository.reload();
            LinkedHashSet<String> ids = new LinkedHashSet<>(repository.getSelectedIds());
            ids.remove("file/xtoxray_custom");
            repository.setSelected(ids);
            mc.reloadResourcePacks();
        } catch (Exception ignored) {
        }
    }

    public static boolean isCustomPackLoaded() {
        return Minecraft.getInstance().getResourcePackRepository().getSelectedIds().contains("file/xtoxray_custom");
    }
}

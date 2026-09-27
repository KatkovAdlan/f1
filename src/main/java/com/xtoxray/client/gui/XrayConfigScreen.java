package com.xtoxray.client.gui;

import com.xtoxray.XrayState;
import com.xtoxray.client.XrayClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.nio.file.Path;
import java.util.List;

public final class XrayConfigScreen extends Screen {
    private static final List<Block> ORE_BLOCKS = List.of(
        Blocks.COAL_ORE,
        Blocks.DEEPSLATE_COAL_ORE,
        Blocks.IRON_ORE,
        Blocks.DEEPSLATE_IRON_ORE,
        Blocks.COPPER_ORE,
        Blocks.DEEPSLATE_COPPER_ORE,
        Blocks.GOLD_ORE,
        Blocks.DEEPSLATE_GOLD_ORE,
        Blocks.EMERALD_ORE,
        Blocks.DEEPSLATE_EMERALD_ORE,
        Blocks.REDSTONE_ORE,
        Blocks.DEEPSLATE_REDSTONE_ORE,
        Blocks.LAPIS_ORE,
        Blocks.DEEPSLATE_LAPIS_ORE,
        Blocks.DIAMOND_ORE,
        Blocks.DEEPSLATE_DIAMOND_ORE,
        Blocks.NETHER_GOLD_ORE,
        Blocks.NETHER_QUARTZ_ORE,
        Blocks.ANCIENT_DEBRIS
    );

    private final Screen parent;
    private Button veinButton;
    private Button customPackButton;

    public XrayConfigScreen(Screen parent) {
        super(Component.literal("X To Xray Configuration"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        XrayState state = XrayState.getInstance();
        int center = width / 2;
        int left = center - 93;
        int top = 72;

        for (int i = 0; i < ORE_BLOCKS.size(); i++) {
            Block block = ORE_BLOCKS.get(i);
            int col = i % 5;
            int row = i / 5;
            int x = left + col * 38;
            int y = top + row * 38;
            addRenderableWidget(Button.builder(Component.literal(shortName(block, state)), button -> {
                state.toggleBlock(block);
                button.setMessage(Component.literal(shortName(block, state)));
                XrayClient.rebuildAll(Minecraft.getInstance());
            }).bounds(x, y, 34, 34).build());
        }

        int controlsY = top + ((ORE_BLOCKS.size() + 4) / 5) * 38 + 8;

        veinButton = addRenderableWidget(Button.builder(
            Component.literal("Vein Miner: " + (state.isVeinMiner() ? "ON" : "OFF")),
            b -> {
                state.setVeinMiner(!state.isVeinMiner());
                b.setMessage(Component.literal("Vein Miner: " + (state.isVeinMiner() ? "ON" : "OFF")));
            }
        ).bounds(center - 100, controlsY, 200, 20).build());

        addRenderableWidget(new RadiusSlider(center - 100, controlsY + 28, 200, 20, state.getOreRenderDistance()));

        customPackButton = addRenderableWidget(Button.builder(
            Component.literal("Custom Pack: " + (state.isCustomPackEnabled() ? "ON" : "OFF")),
            b -> {
                state.setCustomPackEnabled(!state.isCustomPackEnabled());
                if (state.isCustomPackEnabled()) {
                    XrayClient.loadCustomPack();
                } else {
                    XrayClient.unloadCustomPack();
                }
                b.setMessage(Component.literal("Custom Pack: " + (state.isCustomPackEnabled() ? "ON" : "OFF")));
            }
        ).bounds(center - 100, controlsY + 56, 200, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Clear custom pack"), b -> {
            state.setCustomPackPath("");
            state.setCustomPackEnabled(false);
            XrayClient.unloadCustomPack();
            customPackButton.setMessage(Component.literal("Custom Pack: OFF"));
        }).bounds(center - 100, controlsY + 84, 200, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(center - 100, height - 28, 200, 20).build());
    }

    private static String shortName(Block block, XrayState state) {
        String raw = block.toString().toLowerCase();
        String name;
        if (raw.contains("diamond")) name = "Dia";
        else if (raw.contains("emerald")) name = "Em";
        else if (raw.contains("redstone")) name = "Red";
        else if (raw.contains("lapis")) name = "Lap";
        else if (raw.contains("copper")) name = "Cu";
        else if (raw.contains("gold")) name = "Au";
        else if (raw.contains("iron")) name = "Fe";
        else if (raw.contains("coal")) name = "C";
        else if (raw.contains("quartz")) name = "Q";
        else name = "AD";
        return (state.isWhitelisted(block) ? "✓ " : "✗ ") + name;
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        XrayState state = XrayState.getInstance();
        for (Path path : paths) {
            if (path.toString().toLowerCase().endsWith(".zip")) {
                state.setCustomPackPath(path.toAbsolutePath().toString());
                if (state.isCustomPackEnabled()) {
                    XrayClient.loadCustomPack();
                }
                if (customPackButton != null) {
                    customPackButton.setMessage(Component.literal("Custom Pack: " + (state.isCustomPackEnabled() ? "ON" : "OFF")));
                }
                break;
            }
        }
        super.onFilesDrop(paths);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
        graphics.drawCenteredString(font,
            Component.literal("Drop a .zip resource pack onto this screen to set it as the custom pack."),
            width / 2, height - 45, 0xAAAAAA);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private static final class RadiusSlider extends AbstractSliderButton {
        RadiusSlider(int x, int y, int width, int height, int distance) {
            super(x, y, width, height, Component.empty(), (distance - 32) / 480.0D);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int distance = 32 + (int) Math.round(value * 480.0D);
            setMessage(Component.literal("Ore render distance: " + distance + " blocks"));
        }

        @Override
        protected void applyValue() {
            int distance = 32 + (int) Math.round(value * 480.0D);
            XrayState.getInstance().setOreRenderDistance(distance);
            XrayClient.rebuildAll(Minecraft.getInstance());
        }
    }
}

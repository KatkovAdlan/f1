package com.xtoxray.client.gui;

import com.xtoxray.XrayState;
import com.xtoxray.client.ColorXrayRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

import java.util.Locale;

public final class XrayColorPickerScreen extends Screen {
    private static final int PANEL_W = 420;
    private static final int PANEL_H = 250;

    private final Screen parent;
    private final Block block;
    private final XrayState state = XrayState.getInstance();

    private EditBox hexBox;
    private int red;
    private int green;
    private int blue;
    private boolean updatingHex;

    public XrayColorPickerScreen(Screen parent, Block block) {
        super(Component.literal("Цвет блока"));
        this.parent = parent;
        this.block = block;

        int rgb = state.getBlockColor(block);
        red = (rgb >> 16) & 0xFF;
        green = (rgb >> 8) & 0xFF;
        blue = rgb & 0xFF;
    }

    @Override
    protected void init() {
        clearWidgets();

        int left = (width - PANEL_W) / 2;
        int top = Math.max(8, (height - PANEL_H) / 2);
        int x = left + 24;
        int w = PANEL_W - 48;

        addRenderableWidget(new ChannelSlider(x, top + 56, w, 20, 0));
        addRenderableWidget(new ChannelSlider(x, top + 82, w, 20, 1));
        addRenderableWidget(new ChannelSlider(x, top + 108, w, 20, 2));

        hexBox = addRenderableWidget(new EditBox(font, x, top + 140, w, 20, Component.literal("HEX")));
        hexBox.setMaxLength(7);
        hexBox.setValue("#" + toHex());
        hexBox.setHint(Component.literal("HEX, например #00E5FF"));
        hexBox.setResponder(this::readHex);

        addRenderableWidget(Button.builder(
                Component.literal("Сохранить"),
                btn -> saveColor()
        ).bounds(x, top + 174, 120, 22).build());

        addRenderableWidget(Button.builder(
                Component.literal("Сбросить"),
                btn -> {
                    state.resetBlockColor(block);
                    ColorXrayRenderer.requestFullRescan();
                    onClose();
                }
        ).bounds(x + 128, top + 174, 120, 22).build());

        addRenderableWidget(Button.builder(
                Component.literal("Отмена"),
                btn -> onClose()
        ).bounds(x + 256, top + 174, 116, 22).build());
    }

    private void readHex(String value) {
        if (updatingHex) {
            return;
        }

        String cleaned = value.trim();
        if (cleaned.startsWith("#")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.length() != 6 || !cleaned.matches("[0-9a-fA-F]{6}")) {
            return;
        }

        try {
            int rgb = Integer.parseInt(cleaned, 16);
            red = (rgb >> 16) & 0xFF;
            green = (rgb >> 8) & 0xFF;
            blue = rgb & 0xFF;
            syncSliders();
        } catch (NumberFormatException ignored) {
        }
    }

    private void saveColor() {
        state.setBlockColor(block, (red << 16) | (green << 8) | blue);
        ColorXrayRenderer.requestFullRescan();
        onClose();
    }

    private void setChannel(int channel, int value) {
        value = Math.max(0, Math.min(255, value));

        switch (channel) {
            case 0 -> red = value;
            case 1 -> green = value;
            case 2 -> blue = value;
            default -> {
                return;
            }
        }

        if (hexBox != null) {
            updatingHex = true;
            hexBox.setValue("#" + toHex());
            updatingHex = false;
        }
    }

    private int getChannel(int channel) {
        return switch (channel) {
            case 0 -> red;
            case 1 -> green;
            case 2 -> blue;
            default -> 0;
        };
    }

    private String toHex() {
        return String.format(Locale.ROOT, "%02X%02X%02X", red, green, blue);
    }

    private void syncSliders() {
        for (var child : children()) {
            if (child instanceof ChannelSlider slider) {
                slider.syncFromParent();
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x66000000);

        int left = (width - PANEL_W) / 2;
        int top = Math.max(8, (height - PANEL_H) / 2);
        int right = left + PANEL_W;
        int bottom = top + PANEL_H;

        g.fill(left - 3, top - 3, right + 3, bottom + 3, 0x99000000);
        g.fill(left, top, right, bottom, 0xE6080908);
        g.hLine(left, right, top, 0xFF444444);
        g.hLine(left, right, bottom, 0xFF444444);
        g.vLine(left, top, bottom, 0xFF444444);
        g.vLine(right, top, bottom, 0xFF444444);

        g.drawString(font, Component.literal("Цвет: " + block.getName().getString()), left + 12, top + 12, 0xFFFFFFFF, false);
        g.drawString(font, Component.literal("RGB / HEX"), left + 24, top + 39, 0xFF999999, false);

        int color = 0xFF000000 | (red << 16) | (green << 8) | blue;
        g.fill(right - 96, top + 9, right - 12, top + 36, color);
        g.hLine(right - 96, right - 12, top + 9, 0xFFFFFFFF);
        g.hLine(right - 96, right - 12, top + 36, 0xFF444444);
        g.vLine(right - 96, top + 9, top + 36, 0xFFFFFFFF);
        g.vLine(right - 12, top + 9, top + 36, 0xFF444444);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private final class ChannelSlider extends AbstractSliderButton {
        private final int channel;

        ChannelSlider(int x, int y, int w, int h, int channel) {
            super(x, y, w, h, Component.empty(), getChannel(channel) / 255.0D);
            this.channel = channel;
            updateMessage();
        }

        void syncFromParent() {
            value = getChannel(channel) / 255.0D;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            String name = switch (channel) {
                case 0 -> "Красный";
                case 1 -> "Зелёный";
                case 2 -> "Синий";
                default -> "Канал";
            };
            setMessage(Component.literal(name + ": " + getChannel(channel)));
        }

        @Override
        protected void applyValue() {
            setChannel(channel, (int) Math.round(value * 255.0D));
            updateMessage();
        }
    }
}

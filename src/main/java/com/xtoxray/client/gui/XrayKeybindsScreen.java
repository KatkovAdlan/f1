package com.xtoxray.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.xtoxray.client.XrayClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class XrayKeybindsScreen extends Screen {
    private final Screen parent;
    private int listening = -1;

    public XrayKeybindsScreen(Screen parent) {
        super(Component.literal("Настройки клавиш"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = width / 2 - 155;
        int top = 72;

        addKeyRow(left, top, "X-Ray", XrayClient.TOGGLE_KEY.get(), 0);
        addKeyRow(left, top + 48, "VeinMiner", XrayClient.VEIN_MINER_KEY.get(), 1);

        addRenderableWidget(Button.builder(Component.literal("Сбросить клавиши"), b -> {
            reset(XrayClient.TOGGLE_KEY.get());
            reset(XrayClient.VEIN_MINER_KEY.get());
            listening = -1;
            rebuild();
        }).bounds(left, top + 104, 150, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Назад"), b -> onClose())
            .bounds(left + 160, top + 104, 150, 20).build());
    }

    private void addKeyRow(int x, int y, String name, KeyMapping mapping, int index) {
        addRenderableWidget(Button.builder(Component.literal(name), b -> begin(index))
            .bounds(x, y, 150, 20).build());

        Component key = listening == index
            ? Component.literal("Нажмите клавишу...")
            : mapping.getTranslatedKeyMessage();

        addRenderableWidget(Button.builder(key, b -> begin(index))
            .bounds(x + 160, y, 150, 20).build());
    }

    private void begin(int index) {
        listening = index;
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    private KeyMapping mapping(int index) {
        return index == 0 ? XrayClient.TOGGLE_KEY.get() : XrayClient.VEIN_MINER_KEY.get();
    }

    private void setKey(int index, InputConstants.Key key) {
        mapping(index).setKey(key);
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
        listening = -1;
        rebuild();
    }

    private void reset(KeyMapping mapping) {
        mapping.setKey(mapping.getDefaultKey());
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening >= 0) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                listening = -1;
                rebuild();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                setKey(listening, InputConstants.UNKNOWN);
                return true;
            }
            setKey(listening, InputConstants.getKey(keyCode, scanCode));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (listening >= 0) {
            setKey(listening, InputConstants.Type.MOUSE.getOrCreate(button));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        int left = width / 2 - 165;
        int top = 42;
        int right = left + 330;
        int bottom = 208;

        graphics.fill(left - 4, top - 4, right + 4, bottom + 4, 0xCC080808);
        graphics.fill(left, top, right, bottom, 0xE41B1B1B);
        graphics.hLine(left, right, top, 0xFF6A6A6A);
        graphics.hLine(left, right, bottom, 0xFF000000);
        graphics.vLine(left, top, bottom, 0xFF6A6A6A);
        graphics.vLine(right, top, bottom, 0xFF000000);

        graphics.drawCenteredString(font, title, width / 2, top + 9, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.literal("Клавиши управления X to Xray"),
            width / 2, top + 25, 0xAAAAAA);

        graphics.drawString(font,
            Component.literal("Нажмите на поле справа и затем нужную клавишу"),
            left + 10, bottom - 18, 0x888888, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}

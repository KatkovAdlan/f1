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
        int left = width / 2 - 150;
        int top = 62;
        addKeyRow(left, top, "X-Ray", XrayClient.TOGGLE_KEY.get(), 0);
        addKeyRow(left, top + 52, "VeinMiner", XrayClient.VEIN_MINER_KEY.get(), 1);

        addRenderableWidget(Button.builder(Component.literal("Сбросить клавиши"), b -> {
            reset(XrayClient.TOGGLE_KEY.get());
            reset(XrayClient.VEIN_MINER_KEY.get());
        }).bounds(left, top + 116, 145, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Назад"), b -> onClose())
            .bounds(left + 155, top + 116, 145, 20).build());
    }

    private void addKeyRow(int x, int y, String name, KeyMapping mapping, int index) {
        addRenderableWidget(Button.builder(Component.literal(name), b -> beginListening(index))
            .bounds(x, y, 145, 20).build());
        addRenderableWidget(Button.builder(
            listening == index ? Component.literal("Нажмите клавишу...") : mapping.getTranslatedKeyMessage(),
            b -> beginListening(index)
        ).bounds(x + 155, y, 145, 20).build());
    }

    private void beginListening(int index) {
        listening = index;
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
        clearWidgets();
        init();
    }

    private void reset(KeyMapping mapping) {
        mapping.setKey(mapping.getDefaultKey());
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
        listening = -1;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening >= 0) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                listening = -1;
                clearWidgets();
                init();
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
        graphics.drawCenteredString(font, title, width / 2, 18, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.literal("Клавиши действий X to Xray"), width / 2, 34, 0xAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}

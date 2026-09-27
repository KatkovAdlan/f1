package com.xtoxray.client.gui;

import com.xtoxray.XrayState;
import com.xtoxray.client.XrayClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class XrayConfigScreen extends Screen {
    private static final int PANEL_W = 560;
    private static final int PANEL_H = 352;
    private static final int LEFT_W = 220;
    private static final int CELL = 38;
    private static final int GAP = 4;
    private static final int COLS = 5;
    private static final int SELECTED_ROWS = 3;
    private static final int AVAILABLE_ROWS = 7;
    private static final int ROW_H = 30;

    private final Screen parent;
    private final XrayState state = XrayState.getInstance();

    private EditBox searchBox;
    private List<Block> allBlocks = List.of();
    private List<Block> filteredBlocks = List.of();
    private int selectedScroll;
    private int availableScroll;

    public XrayConfigScreen(Screen parent) {
        super(Component.literal("X to Xray"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = left();
        int top = top();
        int w = panelWidth();

        allBlocks = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block != Blocks.AIR) allBlocks.add(block);
        }
        allBlocks.sort(Comparator.comparing(
            (Block block) -> block.getName().getString(),
            String.CASE_INSENSITIVE_ORDER
        ).thenComparing(this::blockId, String.CASE_INSENSITIVE_ORDER));
        rebuildFiltered();

        int rightLeft = left + LEFT_W;
        int rightW = w - LEFT_W;

        searchBox = addRenderableWidget(new EditBox(
            font, rightLeft + 8, top + 27, rightW - 16, 20, Component.literal("Поиск")
        ));
        searchBox.setHint(Component.literal("Поиск блоков..."));
        searchBox.setMaxLength(128);
        searchBox.setResponder(s -> {
            availableScroll = 0;
            rebuildFiltered();
        });

        int controlsY = top + PANEL_H - 67;
        if (height < PANEL_H + 20) controlsY = top + panelHeight() - 67;

        addRenderableWidget(Button.builder(Component.literal(xrayText()), b -> {
            XrayClient.toggleXrayFromGui(Minecraft.getInstance());
            rebuild();
        }).bounds(left + 8, controlsY, 106, 20).build());

        addRenderableWidget(Button.builder(Component.literal(veinText()), b -> {
            XrayClient.toggleVeinMinerFromGui(Minecraft.getInstance());
            rebuild();
        }).bounds(left + 118, controlsY, 106, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Настройки клавиш"), b ->
            Minecraft.getInstance().setScreen(new XrayKeybindsScreen(this))
        ).bounds(left + 228, controlsY, 144, 20).build());

        addRenderableWidget(new DistanceSlider(
            left + 378, controlsY, Math.max(60, w - 386), 20, state.getOreRenderDistance()
        ));

        int actionY = controlsY + 26;
        addRenderableWidget(Button.builder(Component.literal("Добавить из руки"), b -> {
            addHeldBlock();
        }).bounds(left + 8, actionY, 118, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Добавить смотримый"), b -> {
            addLookedAtBlock();
        }).bounds(left + 130, actionY, 130, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Очистить список"), b -> {
            state.clearBlocks();
            selectedScroll = 0;
        }).bounds(left + 264, actionY, 118, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Сбросить"), b -> {
            state.resetDefaults();
            selectedScroll = 0;
            availableScroll = 0;
        }).bounds(left + 386, actionY, Math.max(60, w - 394), 20).build());

        addRenderableWidget(Button.builder(Component.literal("Готово"), b -> onClose())
            .bounds(left + 8, actionY + 26, w - 16, 20).build());
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    private int panelWidth() {
        return Math.min(PANEL_W, Math.max(320, width - 12));
    }

    private int panelHeight() {
        return Math.min(PANEL_H, Math.max(220, height - 12));
    }

    private int left() {
        return (width - panelWidth()) / 2;
    }

    private int top() {
        return Math.max(6, (height - panelHeight()) / 2);
    }

    private void rebuildFiltered() {
        String q = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            filteredBlocks = allBlocks;
            return;
        }
        List<Block> result = new ArrayList<>();
        for (Block block : allBlocks) {
            String name = block.getName().getString().toLowerCase(Locale.ROOT);
            String id = blockId(block).toLowerCase(Locale.ROOT);
            if (name.contains(q) || id.contains(q)) result.add(block);
        }
        filteredBlocks = result;
    }

    private String blockId(Block block) {
        var key = BuiltInRegistries.BLOCK.getKey(block);
        return key == null ? "" : key.toString();
    }

    private String xrayText() {
        return "X-Ray: " + (state.isActive() ? "ВКЛ" : "ВЫКЛ");
    }

    private String veinText() {
        return "VeinMiner: " + (state.isVeinMiner() ? "ВКЛ" : "ВЫКЛ");
    }

    private void addHeldBlock() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Block block = Block.byItem(mc.player.getMainHandItem().getItem());
        if (block != Blocks.AIR) state.addBlock(block);
    }

    private void addLookedAtBlock() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.hitResult instanceof BlockHitResult hit) || mc.level == null) return;
        Block block = mc.level.getBlockState(hit.getBlockPos()).getBlock();
        if (block != Blocks.AIR) state.addBlock(block);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = left();
        int top = top();
        int selectedLeft = left + 6;
        int selectedTop = top + 57;
        int selectedW = LEFT_W - 12;

        if (mouseX >= selectedLeft && mouseX < selectedLeft + selectedW
            && mouseY >= selectedTop
            && mouseY < selectedTop + SELECTED_ROWS * (CELL + GAP)) {
            int col = (int) ((mouseX - selectedLeft) / (CELL + GAP));
            int row = (int) ((mouseY - selectedTop) / (CELL + GAP));
            if (col >= 0 && col < COLS && row >= 0 && row < SELECTED_ROWS) {
                List<Block> selected = state.getWhitelistSorted();
                int index = selectedScroll * COLS + row * COLS + col;
                if (index >= 0 && index < selected.size()) {
                    state.removeBlock(selected.get(index));
                    int max = Math.max(0, (selected.size() + COLS - 1) / COLS - SELECTED_ROWS);
                    selectedScroll = Math.min(selectedScroll, max);
                    return true;
                }
            }
        }

        int availableLeft = left + LEFT_W + 6;
        int availableTop = top + 54;
        int availableW = panelWidth() - LEFT_W - 12;
        if (mouseX >= availableLeft && mouseX < availableLeft + availableW
            && mouseY >= availableTop && mouseY < availableTop + AVAILABLE_ROWS * ROW_H) {
            int row = (int) ((mouseY - availableTop) / ROW_H);
            int index = availableScroll + row;
            if (index >= 0 && index < filteredBlocks.size()) {
                state.toggleBlock(filteredBlocks.get(index));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int left = left();
        int top = top();

        int selectedTop = top + 57;
        if (mouseX >= left + 6 && mouseX < left + LEFT_W - 6
            && mouseY >= selectedTop && mouseY < selectedTop + SELECTED_ROWS * (CELL + GAP)) {
            int rows = (state.getWhitelistSize() + COLS - 1) / COLS;
            int max = Math.max(0, rows - SELECTED_ROWS);
            selectedScroll = clamp(selectedScroll - (int) Math.signum(delta), 0, max);
            return true;
        }

        int availableLeft = left + LEFT_W + 6;
        int availableTop = top + 54;
        int availableW = panelWidth() - LEFT_W - 12;
        if (mouseX >= availableLeft && mouseX < availableLeft + availableW
            && mouseY >= availableTop && mouseY < availableTop + AVAILABLE_ROWS * ROW_H) {
            int max = Math.max(0, filteredBlocks.size() - AVAILABLE_ROWS);
            availableScroll = clamp(availableScroll - (int) Math.signum(delta), 0, max);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(v, max));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        int left = left();
        int top = top();
        int w = panelWidth();
        int h = panelHeight();
        int right = left + w;
        int bottom = top + h;

        graphics.fill(left - 4, top - 4, right + 4, bottom + 4, 0xCC080808);
        graphics.fill(left, top, right, bottom, 0xE41B1B1B);
        graphics.hLine(left, right, top, 0xFF6A6A6A);
        graphics.hLine(left, right, bottom, 0xFF000000);
        graphics.vLine(left, top, bottom, 0xFF6A6A6A);
        graphics.vLine(right, top, bottom, 0xFF000000);

        graphics.drawCenteredString(font, title, width / 2, top + 8, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.literal("Настройка X-Ray"), width / 2, top + 23, 0xAAAAAA);

        int selectedLeft = left + 6;
        int selectedTop = top + 57;
        int selectedW = LEFT_W - 12;

        graphics.drawString(
            font,
            Component.literal("Показывать блоки: " + state.getWhitelistSize()),
            selectedLeft, top + 38, 0xFFFFFF, false
        );
        graphics.fill(selectedLeft, selectedTop, selectedLeft + selectedW,
            selectedTop + SELECTED_ROWS * (CELL + GAP) - GAP, 0xB9080808);

        List<Block> selected = state.getWhitelistSorted();
        int start = selectedScroll * COLS;
        for (int i = 0; i < COLS * SELECTED_ROWS; i++) {
            int index = start + i;
            int col = i % COLS;
            int row = i / COLS;
            int x = selectedLeft + col * (CELL + GAP);
            int y = selectedTop + row * (CELL + GAP);

            graphics.fill(x, y, x + CELL, y + CELL, 0xFF303030);
            if (index >= selected.size()) {
                graphics.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, 0xFF1E1E1E);
                continue;
            }

            Block block = selected.get(index);
            boolean hovered = mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL;
            if (hovered) graphics.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, 0xFF4A4A4A);

            ItemStack stack = block.asItem().getDefaultInstance();
            if (!stack.isEmpty()) graphics.renderItem(stack, x + 11, y + 11);
            graphics.drawString(font, "×", x + CELL - 9, y + 1, 0xFFFFFFFF, false);

            if (hovered && !stack.isEmpty()) graphics.renderTooltip(font, stack, mouseX, mouseY);
        }

        int availableLeft = left + LEFT_W + 6;
        int availableTop = top + 54;
        int availableW = w - LEFT_W - 12;

        graphics.drawString(font, Component.literal("Доступные блоки"), availableLeft, top + 38, 0xFFFFFF, false);
        graphics.fill(availableLeft, availableTop, availableLeft + availableW,
            availableTop + AVAILABLE_ROWS * ROW_H, 0xB9080808);

        for (int row = 0; row < AVAILABLE_ROWS; row++) {
            int index = availableScroll + row;
            if (index >= filteredBlocks.size()) break;

            int y = availableTop + row * ROW_H;
            Block block = filteredBlocks.get(index);
            boolean hovered = mouseX >= availableLeft && mouseX < availableLeft + availableW
                && mouseY >= y && mouseY < y + ROW_H;

            if (hovered) graphics.fill(availableLeft + 1, y + 1,
                availableLeft + availableW - 1, y + ROW_H - 1, 0xFF303030);

            ItemStack stack = block.asItem().getDefaultInstance();
            if (!stack.isEmpty()) graphics.renderItem(stack, availableLeft + 5, y + 6);

            String name = block.getName().getString();
            int max = availableW - 50;
            if (font.width(name) > max) name = font.plainSubstrByWidth(name, Math.max(1, max - 8)) + "…";
            int color = state.isWhitelisted(block) ? 0xFF55CC55 : 0xFFFFFFFF;
            graphics.drawString(font, name, availableLeft + 28, y + 10, color, false);
            graphics.drawString(font, state.isWhitelisted(block) ? "✓" : "+",
                availableLeft + availableW - 16, y + 10, 0xFFFFFFFF, false);

            if (hovered && !stack.isEmpty()) graphics.renderTooltip(font, stack, mouseX, mouseY);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private static final class DistanceSlider extends AbstractSliderButton {
        private DistanceSlider(int x, int y, int width, int height, int distance) {
            super(x, y, width, height, Component.empty(), (distance - 32) / 480.0D);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int distance = 32 + (int) Math.round(value * 480.0D);
            setMessage(Component.literal("Дальность X-Ray: " + distance));
        }

        @Override
        protected void applyValue() {
            int distance = 32 + (int) Math.round(value * 480.0D);
            XrayState.getInstance().setOreRenderDistance(distance);
            XrayClient.rebuildAll(Minecraft.getInstance());
        }
    }
}

package com.xtoxray.client.gui;

import com.xtoxray.XrayState;
import com.xtoxray.client.XrayClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class XrayConfigScreen extends Screen {
    private static final int PANEL_WIDTH = 420;
    private static final int LEFT_WIDTH = 190;
    private static final int RIGHT_WIDTH = 220;
    private static final int ROW_HEIGHT = 28;
    private static final int VISIBLE_ROWS = 8;

    private final Screen parent;
    private final XrayState state = XrayState.getInstance();

    private EditBox searchBox;
    private Button xrayButton;
    private Button veinButton;
    private DistanceSlider distanceSlider;

    private List<Block> allBlocks = List.of();
    private List<Block> filteredBlocks = List.of();

    private int selectedScroll;
    private int availableScroll;

    public XrayConfigScreen(Screen parent) {
        super(Component.translatable("screen.xtoxray.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int panelLeft = (width - PANEL_WIDTH) / 2;
        int panelTop = 36;

        allBlocks = new ArrayList<>();
        for (Block block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            if (block != net.minecraft.world.level.block.Blocks.AIR) {
                allBlocks.add(block);
            }
        }

        allBlocks.sort(Comparator.comparing(
            block -> block.getName().getString(),
            String.CASE_INSENSITIVE_ORDER
        ));
        rebuildFilteredBlocks();

        searchBox = addRenderableWidget(new EditBox(
            font,
            panelLeft + LEFT_WIDTH + 10,
            panelTop + 29,
            RIGHT_WIDTH - 20,
            20,
            Component.translatable("screen.xtoxray.search")
        ));
        searchBox.setHint(Component.translatable("screen.xtoxray.search"));
        searchBox.setMaxLength(128);
        searchBox.setResponder(value -> {
            availableScroll = 0;
            rebuildFilteredBlocks();
        });

        xrayButton = addRenderableWidget(Button.builder(
            xrayMessage(),
            button -> {
                if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) {
                    return;
                }
                XrayClient.toggleXrayFromGui(Minecraft.getInstance());
                button.setMessage(xrayMessage());
            }
        ).bounds(panelLeft, height - 68, 102, 20).build());

        veinButton = addRenderableWidget(Button.builder(
            veinMessage(),
            button -> {
                state.setVeinMiner(!state.isVeinMiner());
                button.setMessage(veinMessage());
            }
        ).bounds(panelLeft + 108, height - 68, 102, 20).build());

        addRenderableWidget(Button.builder(
            Component.translatable("screen.xtoxray.add_held"),
            button -> addHeldBlock()
        ).bounds(panelLeft + 218, height - 68, 202, 20).build());

        addRenderableWidget(Button.builder(
            Component.translatable("screen.xtoxray.add_looked"),
            button -> addLookedAtBlock()
        ).bounds(panelLeft, height - 44, 135, 20).build());

        addRenderableWidget(Button.builder(
            Component.translatable("screen.xtoxray.clear"),
            button -> {
                state.clearBlocks();
                selectedScroll = 0;
            }
        ).bounds(panelLeft + 141, height - 44, 88, 20).build());

        addRenderableWidget(Button.builder(
            Component.translatable("screen.xtoxray.reset"),
            button -> {
                state.resetDefaults();
                selectedScroll = 0;
                availableScroll = 0;
            }
        ).bounds(panelLeft + 235, height - 44, 90, 20).build());

        addRenderableWidget(Button.builder(
            Component.translatable("screen.xtoxray.done"),
            button -> onClose()
        ).bounds(panelLeft + 331, height - 44, 89, 20).build());

        distanceSlider = addRenderableWidget(new DistanceSlider(
            panelLeft,
            height - 92,
            420,
            20,
            state.getOreRenderDistance()
        ));
    }

    private void rebuildFilteredBlocks() {
        String query = searchBox == null
            ? ""
            : searchBox.getValue().trim().toLowerCase(Locale.ROOT);

        if (query.isEmpty()) {
            filteredBlocks = allBlocks;
            return;
        }

        List<Block> result = new ArrayList<>();
        for (Block block : allBlocks) {
            if (block.getName().getString().toLowerCase(Locale.ROOT).contains(query)
                || blockId(block).toLowerCase(Locale.ROOT).contains(query)) {
                result.add(block);
            }
        }
        filteredBlocks = result;
    }

    private String blockId(Block block) {
        var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
        return id == null ? "" : id.toString();
    }

    private Component xrayMessage() {
        return Component.translatable(
            "screen.xtoxray.xray",
            state.isActive()
                ? Component.translatable("screen.xtoxray.on")
                : Component.translatable("screen.xtoxray.off")
        );
    }

    private Component veinMessage() {
        return Component.translatable(
            "screen.xtoxray.vein",
            state.isVeinMiner()
                ? Component.translatable("screen.xtoxray.on")
                : Component.translatable("screen.xtoxray.off")
        );
    }

    private void addHeldBlock() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        ItemStack held = mc.player.getMainHandItem();
        Block block = Block.byItem(held.getItem());
        if (block != null && block != net.minecraft.world.level.block.Blocks.AIR) {
            state.addBlock(block);
        }
    }

    private void addLookedAtBlock() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.hitResult instanceof BlockHitResult hit) || mc.level == null) {
            return;
        }

        Block block = mc.level.getBlockState(hit.getBlockPos()).getBlock();
        if (block != net.minecraft.world.level.block.Blocks.AIR) {
            state.addBlock(block);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelLeft = (width - PANEL_WIDTH) / 2;
        int panelTop = 36;

        if (mouseX >= panelLeft && mouseX < panelLeft + LEFT_WIDTH
            && mouseY >= panelTop + 28 && mouseY < panelTop + 28 + VISIBLE_ROWS * ROW_HEIGHT) {
            int row = (int) ((mouseY - (panelTop + 28)) / ROW_HEIGHT);
            List<Block> selected = state.getWhitelistSorted();
            int index = selectedScroll + row;

            if (index >= 0 && index < selected.size()) {
                state.removeBlock(selected.get(index));
                selectedScroll = Math.min(
                    selectedScroll,
                    Math.max(0, selected.size() - VISIBLE_ROWS - 1)
                );
                return true;
            }
        }

        if (mouseX >= panelLeft + LEFT_WIDTH && mouseX < panelLeft + PANEL_WIDTH
            && mouseY >= panelTop + 52 && mouseY < panelTop + 52 + VISIBLE_ROWS * ROW_HEIGHT) {
            int row = (int) ((mouseY - (panelTop + 52)) / ROW_HEIGHT);
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
        int panelLeft = (width - PANEL_WIDTH) / 2;
        int panelTop = 36;

        if (mouseX >= panelLeft && mouseX < panelLeft + LEFT_WIDTH
            && mouseY >= panelTop + 28 && mouseY < panelTop + 28 + VISIBLE_ROWS * ROW_HEIGHT) {
            selectedScroll = clampScroll(
                selectedScroll - (int) Math.signum(delta),
                state.getWhitelistSize()
            );
            return true;
        }

        if (mouseX >= panelLeft + LEFT_WIDTH && mouseX < panelLeft + PANEL_WIDTH
            && mouseY >= panelTop + 52 && mouseY < panelTop + 52 + VISIBLE_ROWS * ROW_HEIGHT) {
            availableScroll = clampScroll(availableScroll - (int) Math.signum(delta), filteredBlocks.size());
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private int clampScroll(int value, int size) {
        return Math.max(0, Math.min(value, Math.max(0, size - VISIBLE_ROWS)));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        int panelLeft = (width - PANEL_WIDTH) / 2;
        int panelTop = 36;
        int panelRight = panelLeft + PANEL_WIDTH;
        int panelBottom = height - 22;

        graphics.fill(panelLeft - 6, panelTop - 6, panelRight + 6, panelBottom + 4, 0xD90D0D0D);
        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xE61B1B1B);

        graphics.hLine(panelLeft, panelRight, panelTop, 0xFF555555);
        graphics.hLine(panelLeft, panelRight, panelBottom, 0xFF000000);
        graphics.vLine(panelLeft, panelTop, panelBottom, 0xFF555555);
        graphics.vLine(panelRight, panelTop, panelBottom, 0xFF000000);

        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        graphics.drawCenteredString(
            font,
            Component.translatable("screen.xtoxray.subtitle"),
            width / 2,
            24,
            0xAAAAAA
        );

        List<Block> selected = state.getWhitelistSorted();

        drawBlockPanel(
            graphics,
            panelLeft,
            panelTop,
            LEFT_WIDTH,
            selected,
            selectedScroll,
            false,
            mouseX,
            mouseY
        );

        drawBlockPanel(
            graphics,
            panelLeft + LEFT_WIDTH,
            panelTop,
            RIGHT_WIDTH,
            filteredBlocks,
            availableScroll,
            true,
            mouseX,
            mouseY
        );

        graphics.drawCenteredString(
            font,
            Component.translatable(
                "screen.xtoxray.selected",
                state.getWhitelistSize()
            ),
            panelLeft + LEFT_WIDTH / 2,
            panelTop + 9,
            0xFFFFFF
        );

        graphics.drawCenteredString(
            font,
            Component.translatable("screen.xtoxray.available"),
            panelLeft + LEFT_WIDTH + RIGHT_WIDTH / 2,
            panelTop + 9,
            0xFFFFFF
        );

        Component keyHint = Component.translatable(
            "screen.xtoxray.key_hint",
            XrayClient.CONFIG_KEY.get().getTranslatedKeyMessage(),
            XrayClient.TOGGLE_KEY.get().getTranslatedKeyMessage(),
            XrayClient.VEIN_MINER_KEY.get().getTranslatedKeyMessage()
        );
        graphics.drawCenteredString(font, keyHint, width / 2, height - 10, 0x888888);

        super.render(graphics, mouseX, mouseY, partialTick);

        drawHoveredTooltip(graphics, mouseX, mouseY, panelLeft, panelTop);
    }

    private void drawBlockPanel(
        GuiGraphics graphics,
        int left,
        int top,
        int width,
        List<Block> blocks,
        int scroll,
        boolean available,
        int mouseX,
        int mouseY
    ) {
        int listTop = top + (available ? 52 : 28);

        graphics.fill(
            left + 5,
            listTop,
            left + width - 5,
            listTop + VISIBLE_ROWS * ROW_HEIGHT,
            0xCC080808
        );

        graphics.hLine(left + 5, left + width - 5, listTop, 0xFF3A3A3A);

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = scroll + row;
            if (index >= blocks.size()) {
                break;
            }

            Block block = blocks.get(index);
            int rowTop = listTop + row * ROW_HEIGHT;
            boolean hovered = mouseX >= left + 5 && mouseX < left + width - 5
                && mouseY >= rowTop && mouseY < rowTop + ROW_HEIGHT;

            if (hovered) {
                graphics.fill(left + 6, rowTop + 1, left + width - 6, rowTop + ROW_HEIGHT - 1, 0xFF303030);
            }

            if (!available && state.isWhitelisted(block)) {
                graphics.fill(left + 6, rowTop + 1, left + 9, rowTop + ROW_HEIGHT - 1, 0xFF55AA55);
            }

            ItemStack stack = blockItemStack(block);
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, left + 12, rowTop + 5);
            }

            String name = block.getName().getString();
            int maxTextWidth = width - 44;
            if (font.width(name) > maxTextWidth) {
                name = font.plainSubstrByWidth(name, maxTextWidth - 8) + "…";
            }

            graphics.drawString(font, name, left + 36, rowTop + 9, 0xFFFFFF, false);
        }

        if (blocks.isEmpty()) {
            graphics.drawCenteredString(
                font,
                Component.translatable("screen.xtoxray.none"),
                left + width / 2,
                listTop + VISIBLE_ROWS * ROW_HEIGHT / 2 - 4,
                0x888888
            );
        } else if (blocks.size() > VISIBLE_ROWS) {
            drawScrollbar(
                graphics,
                left + width - 7,
                listTop,
                VISIBLE_ROWS * ROW_HEIGHT,
                blocks.size(),
                scroll
            );
        }
    }

    private ItemStack blockItemStack(Block block) {
        Item item = block.asItem();
        return new ItemStack(item);
    }

    private void drawScrollbar(
        GuiGraphics graphics,
        int x,
        int y,
        int height,
        int count,
        int scroll
    ) {
        graphics.fill(x, y, x + 2, y + height, 0xFF202020);

        int thumbHeight = Math.max(12, height * VISIBLE_ROWS / count);
        int maxOffset = height - thumbHeight;
        int maxScroll = Math.max(1, count - VISIBLE_ROWS);
        int thumbY = y + (maxOffset * scroll / maxScroll);

        graphics.fill(x, thumbY, x + 2, thumbY + thumbHeight, 0xFF777777);
    }

    private void drawHoveredTooltip(
        GuiGraphics graphics,
        int mouseX,
        int mouseY,
        int panelLeft,
        int panelTop
    ) {
        int listTopLeft = panelTop + 28;
        int listTopRight = panelTop + 52;

        if (isInsideList(mouseX, mouseY, panelLeft, listTopLeft, LEFT_WIDTH)
            || isInsideList(mouseX, mouseY, panelLeft + LEFT_WIDTH, listTopRight, RIGHT_WIDTH)) {

            boolean right = mouseX >= panelLeft + LEFT_WIDTH;
            int left = right ? panelLeft + LEFT_WIDTH : panelLeft;
            int listTop = right ? listTopRight : listTopLeft;
            int scroll = right ? availableScroll : selectedScroll;
            List<Block> blocks = right ? filteredBlocks : state.getWhitelistSorted();

            int row = (int) ((mouseY - listTop) / ROW_HEIGHT);
            int index = scroll + row;

            if (row >= 0 && row < VISIBLE_ROWS && index >= 0 && index < blocks.size()) {
                ItemStack stack = blockItemStack(blocks.get(index));
                if (!stack.isEmpty()) {
                    graphics.renderTooltip(font, stack, mouseX, mouseY);
                } else {
                    graphics.renderTooltip(
                        font,
                        Component.literal(blockId(blocks.get(index))),
                        mouseX,
                        mouseY
                    );
                }
            }
        }
    }

    private boolean isInsideList(double mouseX, double mouseY, int left, int top, int width) {
        return mouseX >= left + 5
            && mouseX < left + width - 5
            && mouseY >= top
            && mouseY < top + VISIBLE_ROWS * ROW_HEIGHT;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private static final class DistanceSlider extends AbstractSliderButton {
        private DistanceSlider(int x, int y, int width, int height, int distance) {
            super(
                x,
                y,
                width,
                height,
                Component.empty(),
                (distance - 32) / 480.0D
            );
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int distance = 32 + (int) Math.round(value * 480.0D);
            setMessage(Component.translatable("screen.xtoxray.distance", distance));
        }

        @Override
        protected void applyValue() {
            int distance = 32 + (int) Math.round(value * 480.0D);
            XrayState.getInstance().setOreRenderDistance(distance);
            XrayClient.rebuildAll(Minecraft.getInstance());
        }
    }
}

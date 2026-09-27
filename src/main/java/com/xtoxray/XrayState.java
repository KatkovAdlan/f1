package com.xtoxray;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class XrayState {
    private static final XrayState INSTANCE = new XrayState();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Path.of("config", "xtoxray.json");

    private boolean active;
    private boolean veinMiner;
    private int oreRenderDistance = 128;
    private int veinMinerDurabilityPerBlock = 1;
    private final Set<Block> whitelist = new LinkedHashSet<>();

    private XrayState() {
    }

    public static XrayState getInstance() {
        return INSTANCE;
    }

    public void load() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            if (!Files.exists(CONFIG_PATH)) {
                resetDefaults();
                return;
            }

            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                ConfigData data = GSON.fromJson(reader, ConfigData.class);
                if (data == null) {
                    resetDefaults();
                    return;
                }

                oreRenderDistance = clamp(data.oreRenderDistance, 32, 512);
                veinMiner = data.veinMiner;
                veinMinerDurabilityPerBlock = clamp(data.veinMinerDurabilityPerBlock, 1, 10);

                whitelist.clear();
                if (data.whitelist == null) {
                    addDefaultBlocks();
                } else {
                    for (String id : data.whitelist) {
                        ResourceLocation location = ResourceLocation.tryParse(id);
                        if (location != null) {
                            BuiltInRegistries.BLOCK.getOptional(location).ifPresent(whitelist::add);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            resetDefaults();
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            ConfigData data = new ConfigData();
            data.oreRenderDistance = oreRenderDistance;
            data.veinMiner = veinMiner;
            data.veinMinerDurabilityPerBlock = veinMinerDurabilityPerBlock;
            data.whitelist = new ArrayList<>();

            for (Block block : whitelist) {
                ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
                if (key != null) {
                    data.whitelist.add(key.toString());
                }
            }

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException ignored) {
        }
    }

    public void resetDefaults() {
        whitelist.clear();
        addDefaultBlocks();
        oreRenderDistance = 128;
        veinMiner = false;
        veinMinerDurabilityPerBlock = 1;
        save();
    }

    private void addDefaultBlocks() {
        whitelist.add(Blocks.COAL_ORE);
        whitelist.add(Blocks.DEEPSLATE_COAL_ORE);
        whitelist.add(Blocks.IRON_ORE);
        whitelist.add(Blocks.DEEPSLATE_IRON_ORE);
        whitelist.add(Blocks.COPPER_ORE);
        whitelist.add(Blocks.DEEPSLATE_COPPER_ORE);
        whitelist.add(Blocks.GOLD_ORE);
        whitelist.add(Blocks.DEEPSLATE_GOLD_ORE);
        whitelist.add(Blocks.EMERALD_ORE);
        whitelist.add(Blocks.DEEPSLATE_EMERALD_ORE);
        whitelist.add(Blocks.REDSTONE_ORE);
        whitelist.add(Blocks.DEEPSLATE_REDSTONE_ORE);
        whitelist.add(Blocks.LAPIS_ORE);
        whitelist.add(Blocks.DEEPSLATE_LAPIS_ORE);
        whitelist.add(Blocks.DIAMOND_ORE);
        whitelist.add(Blocks.DEEPSLATE_DIAMOND_ORE);
        whitelist.add(Blocks.NETHER_GOLD_ORE);
        whitelist.add(Blocks.NETHER_QUARTZ_ORE);
        whitelist.add(Blocks.ANCIENT_DEBRIS);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        save();
    }

    public void toggle() {
        setActive(!active);
    }

    public boolean shouldRender(BlockState state) {
        return whitelist.contains(state.getBlock());
    }

    public boolean isWhitelisted(Block block) {
        return whitelist.contains(block);
    }

    public void toggleBlock(Block block) {
        if (!whitelist.add(block)) {
            whitelist.remove(block);
        }
        save();
    }

    public void addBlock(Block block) {
        if (block != null && block != Blocks.AIR && whitelist.add(block)) {
            save();
        }
    }

    public void removeBlock(Block block) {
        if (whitelist.remove(block)) {
            save();
        }
    }

    public void clearBlocks() {
        if (!whitelist.isEmpty()) {
            whitelist.clear();
            save();
        }
    }

    public List<Block> getWhitelistSorted() {
        List<Block> result = new ArrayList<>(whitelist);
        result.sort(Comparator.comparing(block -> {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            return id == null ? block.getName().getString() : id.toString();
        }, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public List<Block> getWhitelistInOrder() {
        return new ArrayList<>(whitelist);
    }

    public int getWhitelistSize() {
        return whitelist.size();
    }

    public boolean isVeinMiner() {
        return veinMiner;
    }

    public void setVeinMiner(boolean veinMiner) {
        this.veinMiner = veinMiner;
        save();
    }

    public int getOreRenderDistance() {
        return oreRenderDistance;
    }

    public void setOreRenderDistance(int oreRenderDistance) {
        this.oreRenderDistance = clamp(oreRenderDistance, 32, 512);
        save();
    }

    public int getVeinMinerDurabilityPerBlock() {
        return veinMinerDurabilityPerBlock;
    }

    public void setVeinMinerDurabilityPerBlock(int value) {
        veinMinerDurabilityPerBlock = clamp(value, 1, 10);
        save();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private static final class ConfigData {
        int oreRenderDistance = 128;
        boolean veinMiner;
        int veinMinerDurabilityPerBlock = 1;
        List<String> whitelist;
    }
}

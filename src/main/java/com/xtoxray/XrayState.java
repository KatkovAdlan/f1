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

    private final Set<Block> xrayWhitelist = new LinkedHashSet<>();
    private final Set<Block> veinMinerWhitelist = new LinkedHashSet<>();

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

                xrayWhitelist.clear();
                veinMinerWhitelist.clear();

                // Поддерживаем старый конфиг, где был один общий список.
                List<String> legacy = data.whitelist;
                loadBlocks(xrayWhitelist, data.xrayWhitelist != null ? data.xrayWhitelist : legacy);
                loadBlocks(veinMinerWhitelist, data.veinMinerWhitelist != null ? data.veinMinerWhitelist : legacy);

                if (xrayWhitelist.isEmpty() && data.xrayWhitelist == null && legacy == null) {
                    addDefaultBlocks(xrayWhitelist);
                }
                if (veinMinerWhitelist.isEmpty() && data.veinMinerWhitelist == null && legacy == null) {
                    addDefaultBlocks(veinMinerWhitelist);
                }
            }
        } catch (Exception ignored) {
            resetDefaults();
        }
    }

    private static void loadBlocks(Set<Block> target, List<String> ids) {
        if (ids == null) {
            return;
        }

        for (String id : ids) {
            ResourceLocation location = ResourceLocation.tryParse(id);
            if (location != null) {
                BuiltInRegistries.BLOCK.getOptional(location).ifPresent(target::add);
            }
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            ConfigData data = new ConfigData();
            data.oreRenderDistance = oreRenderDistance;
            data.veinMiner = veinMiner;
            data.veinMinerDurabilityPerBlock = veinMinerDurabilityPerBlock;
            data.xrayWhitelist = toIds(xrayWhitelist);
            data.veinMinerWhitelist = toIds(veinMinerWhitelist);

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private static List<String> toIds(Set<Block> blocks) {
        List<String> ids = new ArrayList<>();
        for (Block block : blocks) {
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
            if (key != null) {
                ids.add(key.toString());
            }
        }
        return ids;
    }

    public void resetDefaults() {
        xrayWhitelist.clear();
        veinMinerWhitelist.clear();
        addDefaultBlocks(xrayWhitelist);
        addDefaultBlocks(veinMinerWhitelist);
        oreRenderDistance = 128;
        veinMiner = false;
        veinMinerDurabilityPerBlock = 1;
        save();
    }

    private static void addDefaultBlocks(Set<Block> target) {
        target.add(Blocks.COAL_ORE);
        target.add(Blocks.DEEPSLATE_COAL_ORE);
        target.add(Blocks.IRON_ORE);
        target.add(Blocks.DEEPSLATE_IRON_ORE);
        target.add(Blocks.COPPER_ORE);
        target.add(Blocks.DEEPSLATE_COPPER_ORE);
        target.add(Blocks.GOLD_ORE);
        target.add(Blocks.DEEPSLATE_GOLD_ORE);
        target.add(Blocks.EMERALD_ORE);
        target.add(Blocks.DEEPSLATE_EMERALD_ORE);
        target.add(Blocks.REDSTONE_ORE);
        target.add(Blocks.DEEPSLATE_REDSTONE_ORE);
        target.add(Blocks.LAPIS_ORE);
        target.add(Blocks.DEEPSLATE_LAPIS_ORE);
        target.add(Blocks.DIAMOND_ORE);
        target.add(Blocks.DEEPSLATE_DIAMOND_ORE);
        target.add(Blocks.NETHER_GOLD_ORE);
        target.add(Blocks.NETHER_QUARTZ_ORE);
        target.add(Blocks.ANCIENT_DEBRIS);
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
        return xrayWhitelist.contains(state.getBlock());
    }

    public boolean isVeinMinerWhitelisted(Block block) {
        return veinMinerWhitelist.contains(block);
    }

    public List<Block> getXrayWhitelistSorted() {
        return sortedCopy(xrayWhitelist);
    }

    public List<Block> getVeinMinerWhitelistSorted() {
        return sortedCopy(veinMinerWhitelist);
    }

    private static List<Block> sortedCopy(Set<Block> blocks) {
        List<Block> result = new ArrayList<>(blocks);
        result.sort(Comparator.comparing(block -> {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            return id == null ? block.getName().getString() : id.toString();
        }, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public int getXrayWhitelistSize() {
        return xrayWhitelist.size();
    }

    public int getVeinMinerWhitelistSize() {
        return veinMinerWhitelist.size();
    }

    public void addXrayBlock(Block block) {
        if (block != null && block != Blocks.AIR && xrayWhitelist.add(block)) {
            save();
        }
    }

    public void addVeinMinerBlock(Block block) {
        if (block != null && block != Blocks.AIR && veinMinerWhitelist.add(block)) {
            save();
        }
    }

    public void removeXrayBlock(Block block) {
        if (xrayWhitelist.remove(block)) {
            save();
        }
    }

    public void removeVeinMinerBlock(Block block) {
        if (veinMinerWhitelist.remove(block)) {
            save();
        }
    }

    public void clearXrayBlocks() {
        if (!xrayWhitelist.isEmpty()) {
            xrayWhitelist.clear();
            save();
        }
    }

    public void clearVeinMinerBlocks() {
        if (!veinMinerWhitelist.isEmpty()) {
            veinMinerWhitelist.clear();
            save();
        }
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
        List<String> xrayWhitelist;
        List<String> veinMinerWhitelist;

        // Поле оставлено только для чтения старых конфигов.
        List<String> whitelist;
    }
}

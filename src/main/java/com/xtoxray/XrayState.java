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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class XrayState {
    private static final XrayState INSTANCE = new XrayState();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Path.of("config", "xtoxray.json");

    private boolean active;
    private boolean veinMiner;
    private int oreRenderDistance = 128;
    private boolean customPackEnabled;
    private String customPackPath = "";
    private final Set<Block> whitelist = new HashSet<>();

    private XrayState() {
    }

    public static XrayState getInstance() {
        return INSTANCE;
    }

    public void load() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            if (!Files.exists(CONFIG_PATH)) {
                initDefaults();
                save();
                return;
            }

            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                ConfigData data = GSON.fromJson(reader, ConfigData.class);
                if (data == null) {
                    initDefaults();
                    save();
                    return;
                }

                oreRenderDistance = clamp(data.oreRenderDistance, 0, 512);
                veinMiner = data.veinMiner;
                customPackEnabled = data.customPackEnabled;
                customPackPath = data.customPackPath == null ? "" : data.customPackPath;

                whitelist.clear();
                if (data.whitelist != null) {
                    for (String id : data.whitelist) {
                        ResourceLocation location = ResourceLocation.tryParse(id);
                        if (location != null) {
                            BuiltInRegistries.BLOCK.getOptional(location).ifPresent(whitelist::add);
                        }
                    }
                }

                if (whitelist.isEmpty()) {
                    initDefaults();
                }
            }
        } catch (Exception ignored) {
            initDefaults();
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            ConfigData data = new ConfigData();
            data.oreRenderDistance = oreRenderDistance;
            data.veinMiner = veinMiner;
            data.customPackEnabled = customPackEnabled;
            data.customPackPath = customPackPath;
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

    private void initDefaults() {
        whitelist.clear();
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
    }

    public void toggle() {
        active = !active;
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

    public Set<Block> getWhitelist() {
        return whitelist;
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
        this.oreRenderDistance = clamp(oreRenderDistance, 0, 512);
        save();
    }

    public boolean isCustomPackEnabled() {
        return customPackEnabled;
    }

    public void setCustomPackEnabled(boolean customPackEnabled) {
        this.customPackEnabled = customPackEnabled;
        save();
    }

    public String getCustomPackPath() {
        return customPackPath;
    }

    public void setCustomPackPath(String customPackPath) {
        this.customPackPath = customPackPath == null ? "" : customPackPath;
        save();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class ConfigData {
        int oreRenderDistance = 128;
        boolean veinMiner;
        boolean customPackEnabled;
        String customPackPath = "";
        List<String> whitelist = new ArrayList<>();
    }
}

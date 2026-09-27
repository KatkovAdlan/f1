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
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;

public final class XrayState {
    private static final XrayState INSTANCE=new XrayState();
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH=Path.of("config","xtoxray.json");
    private boolean active, veinMiner;
    private int oreRenderDistance=128, veinMinerDurabilityPerBlock=1;
    private final Set<Block> whitelist=new LinkedHashSet<>();
    private XrayState(){}
    public static XrayState getInstance(){return INSTANCE;}
    public void load(){
        try{
            Files.createDirectories(CONFIG_PATH.getParent());
            if(!Files.exists(CONFIG_PATH)){resetDefaults();return;}
            try(Reader r=Files.newBufferedReader(CONFIG_PATH)){
                ConfigData d=GSON.fromJson(r,ConfigData.class);
                if(d==null){resetDefaults();return;}
                oreRenderDistance=clamp(d.oreRenderDistance,32,512);
                veinMiner=d.veinMiner;
                veinMinerDurabilityPerBlock=clamp(d.veinMinerDurabilityPerBlock,1,10);
                whitelist.clear();
                if(d.whitelist==null){addDefaultBlocks();save();}
                else for(String id:d.whitelist){ResourceLocation rl=ResourceLocation.tryParse(id);if(rl!=null)BuiltInRegistries.BLOCK.getOptional(rl).ifPresent(whitelist::add);}
            }
        }catch(Exception ignored){resetDefaults();}
    }
    public void save(){
        try{
            Files.createDirectories(CONFIG_PATH.getParent());
            ConfigData d=new ConfigData();
            d.oreRenderDistance=oreRenderDistance; d.veinMiner=veinMiner; d.veinMinerDurabilityPerBlock=veinMinerDurabilityPerBlock; d.whitelist=new ArrayList<>();
            for(Block b:whitelist){ResourceLocation k=BuiltInRegistries.BLOCK.getKey(b);if(k!=null)d.whitelist.add(k.toString());}
            try(Writer w=Files.newBufferedWriter(CONFIG_PATH)){GSON.toJson(d,w);}
        }catch(IOException ignored){}
    }
    public void resetDefaults(){whitelist.clear();addDefaultBlocks();oreRenderDistance=128;veinMiner=false;veinMinerDurabilityPerBlock=1;save();}
    private void addDefaultBlocks(){
        whitelist.add(Blocks.COAL_ORE); whitelist.add(Blocks.DEEPSLATE_COAL_ORE); whitelist.add(Blocks.IRON_ORE); whitelist.add(Blocks.DEEPSLATE_IRON_ORE);
        whitelist.add(Blocks.COPPER_ORE); whitelist.add(Blocks.DEEPSLATE_COPPER_ORE); whitelist.add(Blocks.GOLD_ORE); whitelist.add(Blocks.DEEPSLATE_GOLD_ORE);
        whitelist.add(Blocks.EMERALD_ORE); whitelist.add(Blocks.DEEPSLATE_EMERALD_ORE); whitelist.add(Blocks.REDSTONE_ORE); whitelist.add(Blocks.DEEPSLATE_REDSTONE_ORE);
        whitelist.add(Blocks.LAPIS_ORE); whitelist.add(Blocks.DEEPSLATE_LAPIS_ORE); whitelist.add(Blocks.DIAMOND_ORE); whitelist.add(Blocks.DEEPSLATE_DIAMOND_ORE);
        whitelist.add(Blocks.NETHER_GOLD_ORE); whitelist.add(Blocks.NETHER_QUARTZ_ORE); whitelist.add(Blocks.ANCIENT_DEBRIS);
    }
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;save();} public void toggle(){active=!active;save();}
    public boolean shouldRender(BlockState s){return whitelist.contains(s.getBlock());} public boolean isWhitelisted(Block b){return whitelist.contains(b);}
    public void toggleBlock(Block b){if(!whitelist.add(b))whitelist.remove(b);save();} public void addBlock(Block b){if(b!=Blocks.AIR){whitelist.add(b);save();}}
    public void removeBlock(Block b){if(whitelist.remove(b))save();} public void clearBlocks(){whitelist.clear();save();}
    public List<Block> getWhitelistSorted(){List<Block> r=new ArrayList<>(whitelist);r.sort(Comparator.comparing(b->b.getName().getString(),String.CASE_INSENSITIVE_ORDER));return r;}
    public int getWhitelistSize(){return whitelist.size();}
    public boolean isVeinMiner(){return veinMiner;} public void setVeinMiner(boolean v){veinMiner=v;save();}
    public int getOreRenderDistance(){return oreRenderDistance;} public void setOreRenderDistance(int v){oreRenderDistance=clamp(v,32,512);save();}
    public int getVeinMinerDurabilityPerBlock(){return veinMinerDurabilityPerBlock;} public void setVeinMinerDurabilityPerBlock(int v){veinMinerDurabilityPerBlock=clamp(v,1,10);save();}
    private static int clamp(int v,int min,int max){return Math.max(min,Math.min(v,max));}
    private static final class ConfigData{int oreRenderDistance=128;boolean veinMiner;int veinMinerDurabilityPerBlock=1;List<String> whitelist;}
}
package com.xtoxray;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public final class XrayVeinMiner {
    private static final Queue<Task> TASKS = new ArrayDeque<>();
    private static final ThreadLocal<Boolean> BREAKING_VEIN = ThreadLocal.withInitial(() -> false);

    private XrayVeinMiner() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.register(XrayVeinMiner.class);
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (BREAKING_VEIN.get()) {
            return;
        }

        XrayState state = XrayState.getInstance();
        if (!state.isVeinMiner() || !state.isWhitelisted(event.getState().getBlock())) {
            return;
        }
        if (event.getBreaker() instanceof ServerPlayer player) {
            TASKS.add(new Task(event.getLevel(), player, event.getPos().immutable(), event.getState().getBlock()));
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        int processed = 0;
        while (!TASKS.isEmpty() && processed++ < 16) {
            Task task = TASKS.poll();
            if (task != null) {
                breakVein(task.level, task.player, task.start, task.block);
            }
        }
    }

    private static void breakVein(ServerLevel level, ServerPlayer player, BlockPos start, Block block) {
        final int maxBlocks = 64;
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);

        int found = 0;
        while (!queue.isEmpty() && found < maxBlocks) {
            BlockPos pos = queue.poll();
            found++;
            for (BlockPos neighbor : neighbors(pos)) {
                if (!visited.add(neighbor)) {
                    continue;
                }
                BlockState neighborState = level.getBlockState(neighbor);
                if (neighborState.is(block)) {
                    queue.add(neighbor);
                }
            }
        }

        BREAKING_VEIN.set(true);
        try {
            int broken = 0;
            int durabilityPerBlock = XrayState.getInstance().getVeinMinerDurabilityPerBlock();
            ItemStack tool = player.getMainHandItem();

            for (BlockPos pos : visited) {
                if (pos.equals(start) || broken >= maxBlocks - 1) {
                    continue;
                }

                if (!level.getBlockState(pos).is(block)) {
                    continue;
                }

                if (!player.isCreative() && tool.isEmpty()) {
                    break;
                }

                boolean destroyed = level.destroyBlock(pos, true, player);
                if (!destroyed) {
                    continue;
                }

                broken++;

                if (!player.isCreative() && tool.isDamageableItem()) {
                    tool.hurtAndBreak(durabilityPerBlock, player, EquipmentSlot.MAINHAND);

                    if (tool.isEmpty()) {
                        break;
                    }
                }
            }
        } finally {
            BREAKING_VEIN.set(false);
        }
    }

    private static BlockPos[] neighbors(BlockPos pos) {
        return new BlockPos[] {
            pos.above(), pos.below(), pos.north(), pos.south(), pos.west(), pos.east()
        };
    }

    private record Task(ServerLevel level, ServerPlayer player, BlockPos start, Block block) {
    }
}

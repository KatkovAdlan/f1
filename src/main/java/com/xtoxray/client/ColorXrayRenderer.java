package com.xtoxray.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.xtoxray.XrayState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = "xtoxray", value = Dist.CLIENT)
public final class ColorXrayRenderer {
    public static final RenderType COLOR_OUTLINE = RenderType.create(
        "xtoxray:color_xray_outline",
        DefaultVertexFormat.POSITION_COLOR_NORMAL,
        VertexFormat.Mode.LINES,
        262144,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_LINES_SHADER)
            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
            .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
            .setCullState(RenderStateShard.NO_CULL)
            .setLineState(RenderType.DEFAULT_LINE)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .setOutputState(RenderStateShard.MAIN_TARGET)
            .createCompositeState(false)
    );

    public static final RenderType COLOR_FILL = RenderType.create(
        "xtoxray:color_xray_fill",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        262144,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
            .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
            .setCullState(RenderStateShard.NO_CULL)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .setOutputState(RenderStateShard.MAIN_TARGET)
            .createCompositeState(false)
    );

    private static final Map<Long, List<ColorTarget>> TARGETS_BY_CHUNK = new ConcurrentHashMap<>();
    private static final Set<Long> PENDING_CHUNKS = ConcurrentHashMap.newKeySet();
    private static final Queue<Long> SCAN_QUEUE = new ArrayDeque<>();

    private static long lastWorldId = Long.MIN_VALUE;
    private static int lastPlayerChunkX = Integer.MIN_VALUE;
    private static int lastPlayerChunkZ = Integer.MIN_VALUE;
    private static boolean fullRescanRequested;

    private ColorXrayRenderer() {
    }

    public static void requestFullRescan() {
        fullRescanRequested = true;
    }

    public static void tick(Minecraft mc) {
        XrayState state = XrayState.getInstance();

        if (mc.level == null || mc.player == null || !state.isColorXrayActive()) {
            clearWhenInactive();
            return;
        }

        long worldId = System.identityHashCode(mc.level);
        int chunkX = mc.player.chunkPosition().x;
        int chunkZ = mc.player.chunkPosition().z;

        if (fullRescanRequested || worldId != lastWorldId) {
            fullRescanRequested = false;
            lastWorldId = worldId;
            lastPlayerChunkX = Integer.MIN_VALUE;
            lastPlayerChunkZ = Integer.MIN_VALUE;
            TARGETS_BY_CHUNK.clear();
            SCAN_QUEUE.clear();
            PENDING_CHUNKS.clear();
        }

        if (chunkX != lastPlayerChunkX || chunkZ != lastPlayerChunkZ) {
            lastPlayerChunkX = chunkX;
            lastPlayerChunkZ = chunkZ;
            enqueueChunksAroundPlayer(mc);
        }

        // Прототип намеренно сканирует ограниченное число чанков за тик.
        // Это не даёт первому поиску руд полностью заморозить клиент.
        int budget = 2;
        while (budget-- > 0) {
            Long packedChunk = SCAN_QUEUE.poll();
            if (packedChunk == null) {
                break;
            }
            PENDING_CHUNKS.remove(packedChunk);
            int cx = ChunkPos.getX(packedChunk);
            int cz = ChunkPos.getZ(packedChunk);
            scanChunk(mc, cx, cz);
        }

        removeChunksOutsideRadius(mc);
    }

    private static void enqueueChunksAroundPlayer(Minecraft mc) {
        int radiusChunks = Math.max(1, (mc.player != null ? XrayState.getInstance().getOreRenderDistance() : 128) / 16 + 2);
        ChunkPos center = mc.player.chunkPosition();

        for (int cx = center.x - radiusChunks; cx <= center.x + radiusChunks; cx++) {
            for (int cz = center.z - radiusChunks; cz <= center.z + radiusChunks; cz++) {
                long packed = ChunkPos.asLong(cx, cz);
                if (PENDING_CHUNKS.add(packed) && !TARGETS_BY_CHUNK.containsKey(packed)) {
                    synchronized (SCAN_QUEUE) {
                        SCAN_QUEUE.offer(packed);
                    }
                }
            }
        }
    }

    private static void scanChunk(Minecraft mc, int chunkX, int chunkZ) {
        if (mc.level == null || mc.player == null) {
            return;
        }

        var chunk = mc.level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
        long packedChunk = ChunkPos.asLong(chunkX, chunkZ);
        if (chunk == null) {
            TARGETS_BY_CHUNK.put(packedChunk, Collections.emptyList());
            return;
        }

        XrayState state = XrayState.getInstance();
        int radius = state.getOreRenderDistance();
        long radiusSq = (long) radius * radius;
        int minY = mc.level.getMinBuildHeight();
        int maxY = mc.level.getMaxBuildHeight();
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;

        List<ColorTarget> found = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; localX++) {
            int x = minX + localX;
            for (int localZ = 0; localZ < 16; localZ++) {
                int z = minZ + localZ;
                for (int y = minY; y < maxY; y++) {
                    pos.set(x, y, z);
                    if (pos.distSqr(mc.player.blockPosition()) > radiusSq) {
                        continue;
                    }

                    var blockState = chunk.getBlockState(pos);
                    if (!state.shouldRender(blockState)) {
                        continue;
                    }

                    int color = state.getBlockColor(blockState.getBlock());
                    found.add(new ColorTarget(pos.asLong(), color));
                }
            }
        }

        TARGETS_BY_CHUNK.put(packedChunk, found);
    }

    private static void removeChunksOutsideRadius(Minecraft mc) {
        if (mc.player == null) {
            return;
        }

        int radiusChunks = Math.max(1, XrayState.getInstance().getOreRenderDistance() / 16 + 2);
        ChunkPos center = mc.player.chunkPosition();
        long maxDistanceSq = (long) radiusChunks * radiusChunks;

        TARGETS_BY_CHUNK.keySet().removeIf(packed -> {
            int cx = ChunkPos.getX(packed);
            int cz = ChunkPos.getZ(packed);
            long dx = (long) cx - center.x;
            long dz = (long) cz - center.z;
            return dx * dx + dz * dz > maxDistanceSq * 4L;
        });
    }

    private static void clearWhenInactive() {
        lastWorldId = Long.MIN_VALUE;
        lastPlayerChunkX = Integer.MIN_VALUE;
        lastPlayerChunkZ = Integer.MIN_VALUE;
        TARGETS_BY_CHUNK.clear();
        synchronized (SCAN_QUEUE) {
            SCAN_QUEUE.clear();
        }
        PENDING_CHUNKS.clear();
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        XrayState state = XrayState.getInstance();
        if (!state.isColorXrayActive() || Minecraft.getInstance().player == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        var cameraPos = event.getCamera().getPosition();
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        int alpha = state.getColorOpacity();

        if (state.isColorOutline()) {
            VertexConsumer outline = buffers.getBuffer(COLOR_OUTLINE);
            for (List<ColorTarget> targets : TARGETS_BY_CHUNK.values()) {
                for (ColorTarget target : targets) {
                    drawOutline(poseStack, outline, target.pos(), target.color(), alpha);
                }
            }
            buffers.endBatch(COLOR_OUTLINE);
        }

        if (state.isColorFill()) {
            VertexConsumer fill = buffers.getBuffer(COLOR_FILL);
            for (List<ColorTarget> targets : TARGETS_BY_CHUNK.values()) {
                for (ColorTarget target : targets) {
                    drawFill(poseStack, fill, target.pos(), target.color(), alpha);
                }
            }
            buffers.endBatch(COLOR_FILL);
        }

        poseStack.popPose();
    }

    private static void drawOutline(PoseStack poseStack, VertexConsumer consumer, long packedPos, int rgb, int alpha) {
        BlockPos pos = BlockPos.of(packedPos);
        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;
        LevelRenderer.renderLineBox(
            poseStack,
            consumer,
            pos.getX(), pos.getY(), pos.getZ(),
            pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D,
            red, green, blue, alpha / 255.0F
        );
    }

    private static void drawFill(PoseStack poseStack, VertexConsumer consumer, long packedPos, int rgb, int alpha) {
        BlockPos pos = BlockPos.of(packedPos);
        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;
        LevelRenderer.addChainedFilledBoxVertices(
            poseStack,
            consumer,
            pos.getX(), pos.getY(), pos.getZ(),
            pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D,
            red, green, blue, alpha / 255.0F
        );
    }

    private record ColorTarget(long pos, int color) {
    }
}
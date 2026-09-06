package com.cfmc.client.world;

import com.cfmc.client.protocol.packets.clientbound.ChunkDataPacket;
import com.cfmc.client.util.CFMCLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================================
 * Cesium 格式区块加载器 v0.1 — 接收 ChunkData 包并解析
 * ============================================================================
 *
 * decodeBlockIndices 与服务端 cesium-writer.encodeBlockIndices 互逆:
 *   - palette 下标按 ceil(log2(paletteLen)) bit 定长打包
 *   - 每 64bit (大端 Long) 连续存放, 不跨 Long 对齐 (v0.1 服务端实现
 *     是"无间隔满填", 即 bitOffset 到 64 就换下一个 Long)
 *
 * TODO(Phase 2): 解码结果接入客户端世界渲染
 *   (替换原生 ChunkDataS2CPacket 流程: Mixin 拦截 → 本类灌入 ClientWorld)
 */
public final class CFMCChunkLoader {
    private CFMCChunkLoader() {}

    private static final CFMCChunkLoader INSTANCE = new CFMCChunkLoader();

    public static CFMCChunkLoader getInstance() {
        return INSTANCE;
    }

    /** 已加载的区块缓存: "cx,cz" → sections */
    private final Map<String, Map<Integer, CesiumSection>> loadedChunks = new HashMap<>();

    /** 单个 16³ Section 的内存表示 */
    public static class CesiumSection {
        public final int sectionY;
        /** 全局方块状态 ID 表 (下标即 palette 索引) */
        public final int[] palette;
        /** 4096 个 palette 下标 (Y<<8 | Z<<4 | X) */
        public final short[] indices;
        public final int nonAirBlocks;

        public CesiumSection(int sectionY, int[] palette, short[] indices, int nonAirBlocks) {
            this.sectionY = sectionY;
            this.palette = palette;
            this.indices = indices;
            this.nonAirBlocks = nonAirBlocks;
        }

        /** 取方块状态 ID */
        public int getBlock(int x, int y, int z) {
            int idx = ((y & 0xF) << 8) | ((z & 0xF) << 4) | (x & 0xF);
            return palette[indices[idx]];
        }
    }

    /** 由包解码流程调用 (ChunkDataPacket.decode) */
    public static CesiumSection decodeSection(int sectionY, int[] palette, byte[] packedData, int blockCount) {
        short[] indices = decodeBlockIndices(packedData, palette.length, 4096);
        return new CesiumSection(sectionY, palette, indices, blockCount);
    }

    /** ChunkData 包处理入口 */
    public void handleChunkData(ChunkDataPacket packet) {
        String key = packet.chunkX + "," + packet.chunkZ;
        loadedChunks.put(key, packet.sections);
        CFMCLogger.info("区块 (" + key + ") 已加载: " + packet.sections.size() + " 个非空 Section");
        // TODO(Phase 2): 通知渲染层重建该区块 mesh
    }

    /** 查询任意世界坐标的方块 (跨区块) */
    public int getBlock(int wx, int wy, int wz) {
        Map<Integer, CesiumSection> sections =
                loadedChunks.get((wx >> 4) + "," + (wz >> 4));
        if (sections == null) return 0;
        CesiumSection s = sections.get(Math.floorDiv(wy, 16));
        if (s == null) return 0;
        return s.getBlock(wx, wy, wz);
    }

    /**
     * LongArray (64bit 大端, 无间隔满填) → 调色板下标数组
     * 与服务端 cesium-reader.decodeBlockIndices 逐位一致 (互逆)。
     */
    public static short[] decodeBlockIndices(byte[] packed, int paletteSize, int count) {
        short[] out = new short[count];
        if (paletteSize <= 1 || packed.length == 0) return out; // 单调色板: 全 0

        int bitsPerEntry = (int) Math.ceil(Math.log(paletteSize) / Math.log(2));
        long[] longs = new long[packed.length / 8];
        for (int i = 0; i < longs.length; i++) {
            long v = 0;
            for (int b = 0; b < 8; b++) {
                v = (v << 8) | (packed[i * 8 + b] & 0xFFL);
            }
            longs[i] = v;
        }

        int longIndex = 0;
        int bitOffset = 0;
        for (int i = 0; i < count; i++) {
            int value = 0;
            for (int b = 0; b < bitsPerEntry; b++) {
                if (bitOffset >= 64) {
                    bitOffset = 0;
                    longIndex++;
                }
                long cur = longIndex < longs.length ? longs[longIndex] : 0;
                value = (value << 1) | (int) ((cur >>> (63 - bitOffset)) & 1);
                bitOffset++;
            }
            out[i] = (short) value;
        }
        return out;
    }
}

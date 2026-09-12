package com.cfmc.common.protocol.packets.clientbound;

import com.cfmc.common.platform.CFMCWorldBridge;
import com.cfmc.common.platform.CFMCWorldBridgeHolder;
import com.cfmc.common.world.CFMCChunkLoader;
import com.cfmc.common.util.CFMCConstants;
import com.cfmc.common.protocol.CFMCPacket;
import com.cfmc.common.protocol.PacketReader;
import com.cfmc.common.protocol.PacketWriter;

/**
 * ChunkData (0x03, S→C) — 区块数据 (最重要的包!)
 * payload:
 *   chunkX(I32) chunkZ(I32) fullChunk(Bool) sectionCount(VarInt)
 *   sections[]: sectionY(VarInt) blockCount(U16)
 *               paletteLen(VarInt) palette[](String 方块命名空间名) ← v2: 版本中立!
 *               dataLen(VarInt) data[](64bit大端LongArray打包的调色板索引)
 *
 * v2 变更: 调色板从数字 stateId 改为名字符串 —— 服务端存储与线上协议
 * 都不再绑死任何 MC 版本; 各版本客户端用本版注册表解析成数字 ID。
 *
 * 解码细节见 CFMCChunkLoader.decodeBlockIndices — 与服务端
 * cesium-reader.decodeBlockIndices / cesium-writer.encodeBlockIndices 互逆。
 */
public class ChunkDataPacket extends CFMCPacket {
    public int chunkX;
    public int chunkZ;
    public boolean fullChunk;
    /** 解码后的 sections: sectionY → 调色板 + 4096 个索引 */
    public final java.util.Map<Integer, CFMCChunkLoader.CesiumSection> sections = new java.util.HashMap<>();

    public ChunkDataPacket() {
        super(CFMCConstants.CB_CHUNK_DATA);
    }

    @Override
    public void encode(PacketWriter writer) { /* S2C */ }

    @Override
    public void decode(PacketReader reader) {
        chunkX = reader.readInt();
        chunkZ = reader.readInt();
        fullChunk = reader.readBoolean();
        int sectionCount = reader.readVarInt();

        for (int i = 0; i < sectionCount; i++) {
            int sectionY = reader.readVarInt();
            int blockCount = reader.readUnsignedShort();
            int paletteLen = reader.readVarInt();

            // v2: 调色板是方块名字符串 (版本中立), 解析成数字 ID 由 loader 层负责
            String[] palette = new String[paletteLen];
            for (int p = 0; p < paletteLen; p++) {
                palette[p] = reader.readString(128);
            }

            int dataLen = reader.readVarInt();
            byte[] data = reader.readBytes(dataLen);

            sections.put(sectionY, CFMCChunkLoader.decodeSection(sectionY, palette, data, blockCount));
        }
    }

    @Override
    public void handle() {
        // 内存缓存 (CFMCChunkLoader.getBlock 查询用; 不打日志 — 流式下发时量太大)
        CFMCChunkLoader.getInstance().handleChunkData(this);
        // [Phase 2] 灌入客户端世界渲染 (loader 层实现; IO 线程调用, 实现方自行切主线程)
        CFMCWorldBridge wb = CFMCWorldBridgeHolder.get();
        if (wb != null) wb.onChunkData(chunkX, chunkZ, fullChunk, sections);
    }
}

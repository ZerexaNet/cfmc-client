package com.cfmc.client.protocol.packets.clientbound;

import com.cfmc.client.world.CFMCChunkLoader;
import com.cfmc.client.util.CFMCConstants;
import com.cfmc.client.protocol.CFMCPacket;
import com.cfmc.client.protocol.PacketReader;
import com.cfmc.client.protocol.PacketWriter;

/**
 * ChunkData (0x03, S→C) — 区块数据 (最重要的包!)
 * payload:
 *   chunkX(I32) chunkZ(I32) fullChunk(Bool) sectionCount(VarInt)
 *   sections[]: sectionY(VarInt) blockCount(U16)
 *               paletteLen(VarInt) palette[](VarInt 全局方块状态ID)
 *               dataLen(VarInt) data[](64bit大端LongArray打包的调色板索引)
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

            int[] palette = new int[paletteLen];
            for (int p = 0; p < paletteLen; p++) {
                palette[p] = reader.readVarInt();
            }

            int dataLen = reader.readVarInt();
            byte[] data = reader.readBytes(dataLen);

            sections.put(sectionY, CFMCChunkLoader.decodeSection(sectionY, palette, data, blockCount));
        }
    }

    @Override
    public void handle() {
        // TODO(Phase 2): 灌入客户端世界 (替换 Mixin 拦截到的原生 ChunkData 流程)
        CFMCChunkLoader.getInstance().handleChunkData(this);
    }
}

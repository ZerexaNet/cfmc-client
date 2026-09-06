package com.cfmc.client.protocol.packets.clientbound;

import com.cfmc.client.util.CFMCConstants;
import com.cfmc.client.protocol.CFMCPacket;
import com.cfmc.client.protocol.PacketReader;
import com.cfmc.client.protocol.PacketWriter;

/**
 * BlockUpdate (0x04, S→C) — 单方块变化通知
 * payload: x(I32) y(I32) z(I32) stateId(VarInt)
 */
public class BlockUpdatePacket extends CFMCPacket {
    public int x, y, z;
    public int stateId;

    public BlockUpdatePacket() {
        super(CFMCConstants.CB_BLOCK_UPDATE);
    }

    @Override
    public void encode(PacketWriter writer) { /* S2C */ }

    @Override
    public void decode(PacketReader reader) {
        x = reader.readInt();
        y = reader.readInt();
        z = reader.readInt();
        stateId = reader.readVarInt();
    }

    @Override
    public void handle() {
        // TODO(Phase 2): 更新客户端世界缓存中的单个方块并重渲染该区块段
    }
}

package com.cfmc.common.protocol.packets.clientbound;

import com.cfmc.common.platform.CFMCWorldBridge;
import com.cfmc.common.platform.CFMCWorldBridgeHolder;
import com.cfmc.common.util.CFMCConstants;
import com.cfmc.common.protocol.CFMCPacket;
import com.cfmc.common.protocol.PacketReader;
import com.cfmc.common.protocol.PacketWriter;

/**
 * BlockUpdate (0x04, S→C) — 单方块变化通知
 * payload (v2): x(I32) y(I32) z(I32) blockName(String)
 * v2: stateId → blockName, 版本中立; 各版本客户端自行解析成数字 ID
 */
public class BlockUpdatePacket extends CFMCPacket {
    public int x, y, z;
    public String blockName = CFMCConstants.AIR_BLOCK_NAME;

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
        blockName = reader.readString(128);
    }

    @Override
    public void handle() {
        // [Phase 2] loader 层把 blockName 解析成本版本 BlockState 并重渲染该区块段
        CFMCWorldBridge wb = CFMCWorldBridgeHolder.get();
        if (wb != null) wb.onBlockUpdate(x, y, z, blockName);
    }
}

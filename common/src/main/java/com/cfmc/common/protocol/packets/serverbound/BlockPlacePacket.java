package com.cfmc.common.protocol.packets.serverbound;

import com.cfmc.common.util.CFMCConstants;
import com.cfmc.common.protocol.CFMCPacket;
import com.cfmc.common.protocol.PacketReader;
import com.cfmc.common.protocol.PacketWriter;

/**
 * BlockPlace (0x15, C→S) — 放置方块
 * payload (v2): x(I32) y(I32) z(I32) blockName(String)
 *
 * v2: stateId(数字, 跨版本不稳定) → blockName(命名空间名, 跨版本恒定)。
 * 与原版差异: 原版传"手+朝向+交点"由服务端推断放置格;
 * v0.1 直接传目标格坐标+方块名 (服务端校验物品栏 Phase 3)。
 */
public class BlockPlacePacket extends CFMCPacket {
    public int x, y, z;
    public String blockName;

    public BlockPlacePacket() {
        super(CFMCConstants.SB_BLOCK_PLACE);
    }

    public BlockPlacePacket(int x, int y, int z, String blockName) {
        super(CFMCConstants.SB_BLOCK_PLACE);
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockName = blockName;
    }

    @Override
    public void encode(PacketWriter writer) {
        writer.writeInt(x);
        writer.writeInt(y);
        writer.writeInt(z);
        writer.writeString(blockName == null ? CFMCConstants.AIR_BLOCK_NAME : blockName, 128);
    }

    @Override
    public void decode(PacketReader reader) { /* C2S */ }

    @Override
    public void handle() { /* C2S */ }
}

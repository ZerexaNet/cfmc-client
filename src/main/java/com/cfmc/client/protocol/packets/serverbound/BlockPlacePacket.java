package com.cfmc.client.protocol.packets.serverbound;

import com.cfmc.client.util.CFMCConstants;
import com.cfmc.client.protocol.CFMCPacket;
import com.cfmc.client.protocol.PacketReader;
import com.cfmc.client.protocol.PacketWriter;

/**
 * BlockPlace (0x15, C→S) — 放置方块
 * payload: x(I32) y(I32) z(I32) stateId(VarInt)
 *
 * 与原版差异: 原版传"手+朝向+交点"由服务端推断放置格;
 * v0.1 直接传目标格坐标+方块状态ID (服务端校验物品栏 Phase 3)。
 */
public class BlockPlacePacket extends CFMCPacket {
    public int x, y, z;
    public int stateId;

    public BlockPlacePacket() {
        super(CFMCConstants.SB_BLOCK_PLACE);
    }

    public BlockPlacePacket(int x, int y, int z, int stateId) {
        super(CFMCConstants.SB_BLOCK_PLACE);
        this.x = x;
        this.y = y;
        this.z = z;
        this.stateId = stateId;
    }

    @Override
    public void encode(PacketWriter writer) {
        writer.writeInt(x);
        writer.writeInt(y);
        writer.writeInt(z);
        writer.writeVarInt(stateId);
    }

    @Override
    public void decode(PacketReader reader) { /* C2S */ }

    @Override
    public void handle() { /* C2S */ }
}

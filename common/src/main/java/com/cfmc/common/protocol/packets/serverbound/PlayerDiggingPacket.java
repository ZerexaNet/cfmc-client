package com.cfmc.common.protocol.packets.serverbound;

import com.cfmc.common.util.CFMCConstants;
import com.cfmc.common.protocol.CFMCPacket;
import com.cfmc.common.protocol.PacketReader;
import com.cfmc.common.protocol.PacketWriter;

/**
 * PlayerDigging (0x14, C→S) — 挖掘方块 (三态)
 * payload: status(U8: 0=开始 1=取消 2=完成) | x(I32) y(I32) z(I32)
 *
 * v0.1 即挖即碎: 本地一击完成 → 直接发 status=2。
 * 完整挖掘时间/工具耐久 Phase 3。
 */
public class PlayerDiggingPacket extends CFMCPacket {
    public static final int STATUS_START = 0;
    public static final int STATUS_CANCEL = 1;
    public static final int STATUS_FINISHED = 2;

    public int status;
    public int x, y, z;

    public PlayerDiggingPacket() {
        super(CFMCConstants.SB_PLAYER_DIGGING);
    }

    public PlayerDiggingPacket(int status, int x, int y, int z) {
        super(CFMCConstants.SB_PLAYER_DIGGING);
        this.status = status;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void encode(PacketWriter writer) {
        writer.writeUnsignedByte(status);
        writer.writeInt(x);
        writer.writeInt(y);
        writer.writeInt(z);
    }

    @Override
    public void decode(PacketReader reader) { /* C2S */ }

    @Override
    public void handle() { /* C2S */ }
}

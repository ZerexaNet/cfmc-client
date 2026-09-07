package com.cfmc.common.protocol.packets.serverbound;

import com.cfmc.common.util.CFMCConstants;
import com.cfmc.common.protocol.CFMCPacket;
import com.cfmc.common.protocol.PacketReader;
import com.cfmc.common.protocol.PacketWriter;

/**
 * PlayerPositionLook (0x13, C→S) — 位置+视角合并包 (每tick, 推荐用这个减少包数)
 * payload: x(D) feetY(D) z(D) yaw(F) pitch(F) onGround(Bool) flags(U8)
 *
 * 客户端预测策略 (cfmc.md 注意事项 #3):
 *   不要等服务端确认才移动! 本地先动, 服务端校正后回拉。
 *   v0.1 只发绝对坐标 (flags=0); 相对差量编码 Phase 2 启用 (省约60%带宽)。
 *
 * 与原版差异: 原版每 tick 走 PlayerMoveC2SPacket 家族; 我们合并成单包。
 */
public class PlayerPositionLookPacket extends CFMCPacket {
    public double x, feetY, z;
    public float yaw, pitch;
    public boolean onGround;
    /** 位掩码: bit0-2 XYZ相对 / bit3-4 视角相对 (v0.1 恒 0) */
    public int flags;

    public PlayerPositionLookPacket() {
        super(CFMCConstants.SB_PLAYER_POSITION_LOOK);
    }

    public PlayerPositionLookPacket(double x, double feetY, double z, float yaw, float pitch, boolean onGround) {
        super(CFMCConstants.SB_PLAYER_POSITION_LOOK);
        this.x = x;
        this.feetY = feetY;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.flags = 0;
    }

    @Override
    public void encode(PacketWriter writer) {
        writer.writeDouble(x);
        writer.writeDouble(feetY);
        writer.writeDouble(z);
        writer.writeFloat(yaw);
        writer.writeFloat(pitch);
        writer.writeBoolean(onGround);
        writer.writeUnsignedByte(flags);
    }

    @Override
    public void decode(PacketReader reader) { /* C2S */ }

    @Override
    public void handle() { /* C2S */ }
}

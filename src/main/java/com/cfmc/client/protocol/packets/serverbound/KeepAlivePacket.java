package com.cfmc.client.protocol.packets.serverbound;

import com.cfmc.client.util.CFMCConstants;
import com.cfmc.client.protocol.CFMCPacket;
import com.cfmc.client.protocol.PacketReader;
import com.cfmc.client.protocol.PacketWriter;

/**
 * KeepAlive (0x18, C→S) — 心跳应答 (每10秒, 收到 CB_KEEP_ALIVE 后立即回)
 * payload: 空
 * 超过 30 秒未回 → 服务端判定死链踢出。
 */
public class KeepAlivePacket extends CFMCPacket {
    public KeepAlivePacket() {
        super(CFMCConstants.SB_KEEP_ALIVE);
    }

    @Override
    public void encode(PacketWriter writer) { /* 空 payload */ }

    @Override
    public void decode(PacketReader reader) { /* C2S */ }

    @Override
    public void handle() { /* C2S */ }
}

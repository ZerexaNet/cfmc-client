package com.cfmc.client.protocol.packets.clientbound;

import com.cfmc.client.network.CFMCNetworkManager;
import com.cfmc.client.util.CFMCConstants;
import com.cfmc.client.protocol.CFMCPacket;
import com.cfmc.client.protocol.PacketReader;
import com.cfmc.client.protocol.PacketWriter;

/**
 * KeepAlive (0x0B, S→C) — 服务端心跳探测 (每10秒)
 * payload: 空
 * 客户端收到后必须立即回 SB_KEEP_ALIVE, 并刷新延迟统计。
 */
public class KeepAlivePacket extends CFMCPacket {
    /** 服务端发出 → 客户端收到的时间差, 用于 HUD 延迟显示 */
    public static volatile long lastPingMs = -1;

    public KeepAlivePacket() {
        super(CFMCConstants.CB_KEEP_ALIVE);
    }

    @Override
    public void encode(PacketWriter writer) { /* S2C */ }

    @Override
    public void decode(PacketReader reader) { /* 空 payload */ }

    @Override
    public void handle() {
        // 立即回应 (v0.1 延迟=请求间隔; Phase 3: 包内加时间戳算真实 RTT)
        CFMCNetworkManager.getInstance().sendPacket(new com.cfmc.client.protocol.packets.serverbound.KeepAlivePacket());
        CFMCNetworkManager.getInstance().onServerKeepAlive();
    }
}

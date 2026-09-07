package com.cfmc.common.protocol.packets.clientbound;

import com.cfmc.common.network.CFMCNetworkManager;
import com.cfmc.common.util.CFMCConstants;
import com.cfmc.common.protocol.CFMCPacket;
import com.cfmc.common.protocol.PacketReader;
import com.cfmc.common.protocol.PacketWriter;

/**
 * Disconnect (0x0A, S→C) — 服务端主动断开 (带原因)
 * payload: reason(String)
 * 优先级: HIGH (插队发送)
 */
public class DisconnectPacket extends CFMCPacket {
    public String reason;

    public DisconnectPacket() {
        super(CFMCConstants.CB_DISCONNECT);
    }

    @Override
    public void encode(PacketWriter writer) { /* S2C */ }

    @Override
    public void decode(PacketReader reader) {
        reason = reader.readString(256);
    }

    @Override
    public void handle() {
        // 主动关闭连接并展示断开界面 (原因带到 CFMCDisconnectScreen)
        CFMCNetworkManager.getInstance().onServerDisconnect(reason);
    }
}

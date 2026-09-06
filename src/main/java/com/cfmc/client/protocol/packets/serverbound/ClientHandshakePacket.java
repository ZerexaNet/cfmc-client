package com.cfmc.client.protocol.packets.serverbound;

import com.cfmc.client.util.CFMCConstants;
import com.cfmc.client.protocol.CFMCPacket;
import com.cfmc.client.protocol.PacketReader;
import com.cfmc.client.protocol.PacketWriter;

/**
 * ClientHandshake (0x10, C→S) — 握手请求 (连接后第一个包)
 * payload: protocolVersion(VarInt) | playerName(String≤16)
 *
 * 认证上下文: JWT 由 HTTP 升级请求头 Authorization: Bearer 携带
 * (见 CFMCNetworkManager.connect), 因此二进制握手只需版本协商。
 */
public class ClientHandshakePacket extends CFMCPacket {
    public String playerName;

    public ClientHandshakePacket(String playerName) {
        super(CFMCConstants.SB_CLIENT_HANDSHAKE);
        this.playerName = playerName;
    }

    public ClientHandshakePacket() {
        super(CFMCConstants.SB_CLIENT_HANDSHAKE);
    }

    @Override
    public void encode(PacketWriter writer) {
        writer.writeVarInt(CFMCConstants.PROTOCOL_VERSION);
        writer.writeString(playerName, 16);
    }

    @Override
    public void decode(PacketReader reader) { /* C2S 不解码 */ }

    @Override
    public void handle() { /* C2S */ }
}

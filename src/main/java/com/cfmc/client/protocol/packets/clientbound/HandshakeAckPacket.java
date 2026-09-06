package com.cfmc.client.protocol.packets.clientbound;

import com.cfmc.client.network.CFMCNetworkManager;
import com.cfmc.client.protocol.CFMCPacket;
import com.cfmc.client.protocol.PacketReader;
import com.cfmc.client.protocol.PacketWriter;
import com.cfmc.client.util.CFMCConstants;
import com.cfmc.client.util.CFMCLogger;

/**
 * HandshakeAck (0x01, S→C) — 握手确认 + 协商参数
 * payload: protocolVersion(VarInt) | regionX(I32) | regionZ(I32) | viewDistance(U8)
 * 与服务端 RegionDO.#handleConnect 的 ack 编码逐字段一致。
 */
public class HandshakeAckPacket extends CFMCPacket {
    public int protocolVersion;
    public int regionX;
    public int regionZ;
    public int viewDistance;

    public HandshakeAckPacket() {
        super(CFMCConstants.CB_HANDSHAKE_ACK);
    }

    @Override
    public void encode(PacketWriter writer) { /* S2C 不编码 */ }

    @Override
    public void decode(PacketReader reader) {
        protocolVersion = reader.readVarInt();
        regionX = reader.readInt();
        regionZ = reader.readInt();
        viewDistance = reader.readUnsignedByte();
    }

    @Override
    public void handle() {
        // 协议版本协商: 不一致则提示更新 (不自动断开, 交由玩家决定)
        if (protocolVersion != CFMCConstants.PROTOCOL_VERSION) {
            CFMCLogger.warn("协议版本不匹配: 服务端 " + protocolVersion
                    + " / 客户端 " + CFMCConstants.PROTOCOL_VERSION);
        }
        CFMCNetworkManager.getInstance().onHandshakeAck(this);
    }
}

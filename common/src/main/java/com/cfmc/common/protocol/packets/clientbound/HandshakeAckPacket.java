package com.cfmc.common.protocol.packets.clientbound;

import com.cfmc.common.network.CFMCNetworkManager;
import com.cfmc.common.protocol.CFMCPacket;
import com.cfmc.common.protocol.PacketReader;
import com.cfmc.common.protocol.PacketWriter;
import com.cfmc.common.util.CFMCConstants;
import com.cfmc.common.util.CFMCLogger;

/**
 * HandshakeAck (0x01, S→C) — 握手确认 + 协商参数
 *
 * payload (v2):
 *   protocolVersion(VarInt)  regionX(I32)  regionZ(I32)  viewDistance(U8)
 *   adapter(String)          服务端选定的版本适配器名 (legacy/flat/modern/generic) ← v2
 *   mcProtoMin(VarInt)       服务端支持的最早 MC 协议号                    ← v2
 *   mcProtoMax(VarInt)       服务端支持的最新 MC 协议号                    ← v2
 *
 * 尾部追加设计: 老客户端读到 viewDistance 即停, 多余字节无害。
 * 服务端在收到 ClientHandshake 后会补发第二次 ACK (带精确适配器),
 * 本类幂等处理 (重复调用只更新字段)。
 */
public class HandshakeAckPacket extends CFMCPacket {
    public int protocolVersion;
    public int regionX;
    public int regionZ;
    public int viewDistance;
    public String adapterName = "generic";   // v2: 未读到时兜底
    public int mcProtoMin = 47;              // v2: 未读到时按全范围兜底
    public int mcProtoMax = 772;

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
        try {
            // v2 尾部字段 (旧服务端无 → EOF 异常, 兑底值已设好)
            adapterName = reader.readString(16);
            mcProtoMin = reader.readVarInt();
            mcProtoMax = reader.readVarInt();
        } catch (Exception ignored) { /* v1 服务端 */ }
    }

    @Override
    public void handle() {
        // CFMC 协议版本协商: 不一致则提示 (不自动断开, 交由玩家决定)
        if (protocolVersion != CFMCConstants.PROTOCOL_VERSION) {
            CFMCLogger.warn("CFMC 协议版本不匹配: 服务端 v" + protocolVersion
                    + " / 客户端 v" + CFMCConstants.PROTOCOL_VERSION);
        }
        CFMCNetworkManager.getInstance().onHandshakeAck(this);
    }
}

package com.cfmc.client.protocol;

import com.cfmc.client.protocol.packets.clientbound.BlockUpdatePacket;
import com.cfmc.client.protocol.packets.clientbound.ChatMessagePacket;
import com.cfmc.client.protocol.packets.clientbound.ChunkDataPacket;
import com.cfmc.client.protocol.packets.clientbound.DisconnectPacket;
import com.cfmc.client.protocol.packets.clientbound.EntityMovePacket;
import com.cfmc.client.protocol.packets.clientbound.HandshakeAckPacket;
import com.cfmc.client.protocol.packets.clientbound.JoinGamePacket;
import com.cfmc.client.protocol.packets.clientbound.KeepAlivePacket;
import com.cfmc.client.protocol.packets.clientbound.PlayerInfoPacket;
import com.cfmc.client.util.CFMCConstants;
import com.cfmc.client.util.CFMCLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * ============================================================================
 * 包注册表 — 包ID ↔ Java类 的双向映射
 * ============================================================================
 * 对齐文件: cfmc-server/src/protocol/packet-definitions.js
 * 未注册的 S2C 包静默丢弃 (向前兼容: 旧客户端连新服务端不崩)。
 */
public final class CFMCPacketRegistry {
    private CFMCPacketRegistry() {}

    /** S2C: 包ID → 实例工厂 (decode 前先 new, 字段由 decode 填充) */
    private static final Map<Integer, Supplier<? extends CFMCPacket>> S2C = new HashMap<>();

    static {
        // ---- Clientbound (v0.1 P0 集; 其余包 Phase 2 补) ----
        S2C.put(CFMCConstants.CB_HANDSHAKE_ACK, HandshakeAckPacket::new);
        S2C.put(CFMCConstants.CB_JOIN_GAME, JoinGamePacket::new);
        S2C.put(CFMCConstants.CB_CHUNK_DATA, ChunkDataPacket::new);
        S2C.put(CFMCConstants.CB_BLOCK_UPDATE, BlockUpdatePacket::new);
        S2C.put(CFMCConstants.CB_ENTITY_MOVE, EntityMovePacket::new);
        S2C.put(CFMCConstants.CB_PLAYER_INFO, PlayerInfoPacket::new);
        S2C.put(CFMCConstants.CB_CHAT_MESSAGE, ChatMessagePacket::new);
        S2C.put(CFMCConstants.CB_DISCONNECT, DisconnectPacket::new);
        S2C.put(CFMCConstants.CB_KEEP_ALIVE, KeepAlivePacket::new);
    }

    /** 由包ID实例化 S2C 包; 未知包返回 null (调用方丢弃) */
    public static CFMCPacket createClientbound(int packetId) {
        Supplier<? extends CFMCPacket> factory = S2C.get(packetId);
        if (factory == null) {
            CFMCLogger.warn("收到未注册的包 ID: 0x" + Integer.toHexString(packetId) + " (忽略)");
            return null;
        }
        return factory.get();
    }
}

package com.cfmc.client.util;

/**
 * ============================================================================
 * 协议与服务端常量 — 必须与 cfmc-server/src/protocol/packet-definitions.js 逐字一致!
 * ============================================================================
 * 变更规则 (cfmc.md 要求 #10): 任何一端变更都要同步另一端并递增 PROTOCOL_VERSION。
 */
public final class CFMCConstants {
    private CFMCConstants() {}

    /** 协议版本 (服务端 PacketReader/PROTOCOL_VERSION 对齐) */
    public static final int PROTOCOL_VERSION = 1;

    /** 单包大小上限 (1 MiB, 与服务端一致) */
    public static final int MAX_PACKET_SIZE = 1 << 20;

    /** 超过此大小的 Payload 才压缩 (字节) */
    public static final int COMPRESSION_THRESHOLD = 256;

    /** Flags 位域 */
    public static final int FLAG_COMPRESSED = 0x01;
    public static final int FLAG_ENCRYPTED = 0x02;
    public static final int FLAG_HIGH_PRIORITY = 0x04;

    /** Clientbound 包 ID (S→C) */
    public static final int CB_HANDSHAKE_ACK = 0x01;
    public static final int CB_JOIN_GAME = 0x02;
    public static final int CB_CHUNK_DATA = 0x03;
    public static final int CB_BLOCK_UPDATE = 0x04;
    public static final int CB_ENTITY_SPAWN = 0x05;
    public static final int CB_ENTITY_MOVE = 0x06;
    public static final int CB_ENTITY_DESTROY = 0x07;
    public static final int CB_PLAYER_INFO = 0x08;
    public static final int CB_CHAT_MESSAGE = 0x09;
    public static final int CB_DISCONNECT = 0x0A;
    public static final int CB_KEEP_ALIVE = 0x0B;

    /** Serverbound 包 ID (C→S) */
    public static final int SB_CLIENT_HANDSHAKE = 0x10;
    public static final int SB_PLAYER_POSITION = 0x11;
    public static final int SB_PLAYER_LOOK = 0x12;
    public static final int SB_PLAYER_POSITION_LOOK = 0x13;
    public static final int SB_PLAYER_DIGGING = 0x14;
    public static final int SB_BLOCK_PLACE = 0x15;
    public static final int SB_CHAT_MESSAGE = 0x17;
    public static final int SB_KEEP_ALIVE = 0x18;
}

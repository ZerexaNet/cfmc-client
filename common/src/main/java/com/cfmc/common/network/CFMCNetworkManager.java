package com.cfmc.common.network;

import com.cfmc.common.config.CFMCConfig;
import com.cfmc.common.auth.AuthResult;
import com.cfmc.common.protocol.CFMCPacket;
import com.cfmc.common.protocol.CFMCBufferUtils;
import com.cfmc.common.protocol.CFMCPacketRegistry;
import com.cfmc.common.protocol.PacketReader;
import com.cfmc.common.protocol.PacketWriter;
import com.cfmc.common.protocol.packets.clientbound.HandshakeAckPacket;
import com.cfmc.common.protocol.packets.clientbound.JoinGamePacket;
import com.cfmc.common.protocol.packets.clientbound.ChunkDataPacket;
import com.cfmc.common.protocol.packets.serverbound.ClientHandshakePacket;
import com.cfmc.common.util.CFMCConstants;
import com.cfmc.common.util.CFMCLogger;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ============================================================================
 * CFMC 网络管理器 (单例) — 协议状态机 + 帧解析 + 包分派
 * ============================================================================
 *
 * 对齐: 服务端 game.js (HTTP升级/身份) + RegionDO (二进制协议)
 *
 * 连接流程:
 *   1. connect(serverUrl, authResult): 建 WSS, Header 携带 Bearer JWT
 *   2. onOpen → 发 ClientHandshake (版本协商)
 *   3. onHandshakeAck → 版本校验, 就绪
 *   4. onJoinGame → 进入世界 (HUD 显示已连接)
 *
 * 帧解析 (与服务端 webSocketMessage 对称):
 *   一条 WS 二进制消息 = 连续多个帧 [Length|PacketID|Flags|Payload]...
 *   Flags & COMPRESSED → deflate-raw 解压 (Java Inflater nowrap=true)
 */
public final class CFMCNetworkManager {
    private static final CFMCNetworkManager INSTANCE = new CFMCNetworkManager();

    public static CFMCNetworkManager getInstance() {
        return INSTANCE;
    }

    private CFMCNetworkManager() {}

    /** 连接状态机 */
    public enum State { DISCONNECTED, CONNECTING, HANDSHAKING, IN_GAME }

    private volatile State state = State.DISCONNECTED;
    private volatile String currentRegion = "0,0";
    private volatile long lastServerKeepAliveMs = 0;

    private CFMCWebSocketClient websocket;
    private final AtomicBoolean connecting = new AtomicBoolean(false);

    /** 服务端 Disconnect 包已提示标记 (防 onClose 二次通知; connect 时复位) */
    private volatile boolean serverDisconnectNotified = false;

    /** 当前认证结果 (重连复用) */
    private AuthResult lastAuth;

    /* =========================================================================
     * 连接管理
     * ======================================================================= */

    /**
     * 连接 CFMC 服务器
     * @param serverUrl 如 ws://localhost:8787 或 wss://cfmc.example.com
     * @param auth      认证结果 (JWT); null = 匿名 (开发模式)
     */
    public void connect(String serverUrl, AuthResult auth) {
        if (!connecting.compareAndSet(false, true)) return; // 防重复连接
        this.lastAuth = auth;

        try {
            URI uri = URI.create(serverUrl.replaceAll("/+$", "") + "/ws/game?region=0,0");

            Map<String, String> headers = new HashMap<>();
            if (auth != null && auth.accessToken != null) {
                headers.put("Authorization", "Bearer " + auth.accessToken);
            }

            state = State.CONNECTING;
            serverDisconnectNotified = false;
            CFMCLogger.info("连接 " + uri);

            websocket = new CFMCWebSocketClient(uri, headers, this);
            websocket.connect(); // 异步; 结果回调 onOpen/onError
        } catch (Exception e) {
            CFMCLogger.error("连接失败", e);
            state = State.DISCONNECTED;
            connecting.set(false);
        }
    }

    /** 断开并清理 */
    public void disconnect() {
        if (websocket != null) {
            websocket.close();
            websocket = null;
        }
        state = State.DISCONNECTED;
        connecting.set(false);
        CFMCHeartbeatManager.getInstance().stop();
    }

    public boolean isActive() {
        return state == State.HANDSHAKING || state == State.IN_GAME;
    }

    public State getState() {
        return state;
    }

    public String getCurrentRegion() {
        return currentRegion;
    }

    /* =========================================================================
     * 发送 (C2S)
     * ======================================================================= */

    /** 发送一个包 (帧封装在此完成; >阈值压缩 TODO: 与服务端协商后启用) */
    public void sendPacket(CFMCPacket packet) {
        if (websocket == null || !websocket.isOpen()) return;
        PacketWriter w = new PacketWriter(64);
        packet.encode(w);
        byte[] frame = PacketWriter.frame(packet.getPacketId(), 0, w.toArray());
        websocket.send(frame);
    }

    /* =========================================================================
     * 接收回调 (由 CFMCWebSocketClient 的 IO 线程调用)
     * ======================================================================= */

    /** 连接建立 → 立即握手 */
    public void onOpen(int httpStatus) {
        state = State.HANDSHAKING;
        CFMCLogger.info("WebSocket 已建立 (HTTP " + httpStatus + "), 发送握手");
        sendPacket(new ClientHandshakePacket(lastAuth != null ? lastAuth.name : "Player"));
        CFMCHeartbeatManager.getInstance().start();
    }

    /** 文本 JSON 调试通道 */
    public void onTextMessage(String message) {
        CFMCLogger.info("[服务端文本] " + message);
    }

    /** 二进制消息 → 帧循环 → 包分派 (协议核心!) */
    public void onBinaryMessage(byte[] data) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(data);
            int guard = 0;
            while (buf.remaining() > 0 && guard++ < 256) {
                int length = CFMCBufferUtils.readVarInt(buf);
                if (length <= 0 || length > CFMCConstants.MAX_PACKET_SIZE) {
                    throw new RuntimeException("非法帧长度: " + length);
                }
                if (buf.remaining() < length) throw new RuntimeException("帧不完整");

                // 切出帧体 (PacketID + Flags + Payload)
                byte[] frame = new byte[length];
                buf.get(frame);

                PacketReader fr = new PacketReader(frame);
                int packetId = fr.readVarInt();
                int flags = fr.readUnsignedByte();
                byte[] payload = new byte[fr.remaining()];
                fr.raw().get(payload);

                if ((flags & CFMCConstants.FLAG_COMPRESSED) != 0) {
                    payload = CFMCCompression.inflateRaw(payload);
                }

                CFMCPacket packet = CFMCPacketRegistry.createClientbound(packetId);
                if (packet != null) {
                    packet.decode(new PacketReader(payload));
                    // TODO(Phase 2): 经 MinecraftClient.execute 切主线程后 handle()
                    packet.handle();
                }
            }
        } catch (Exception e) {
            CFMCLogger.error("包解析失败 (连接保持)", e);
        }
    }

    public void onClose(int code, String reason, boolean remote) {
        CFMCLogger.info("连接关闭: code=" + code + " remote=" + remote + " reason=" + reason);
        state = State.DISCONNECTED;
        connecting.set(false);
        CFMCHeartbeatManager.getInstance().stop();

        // [Phase 2] 异常关闭通知 loader 层 (服务端 Disconnect 包路径已提示过的不再重复)
        boolean cleanLocal = code == 1000 && !remote;
        if (!serverDisconnectNotified && !cleanLocal) {
            com.cfmc.common.platform.CFMCWorldBridge wb =
                    com.cfmc.common.platform.CFMCWorldBridgeHolder.get();
            if (wb != null) wb.onConnectionClosed(code, reason, remote);
        }

        // 自动重连 (配置开启且非主动断开)
        if (CFMCConfig.get().autoReconnect && remote) {
            CFMCReconnectHandler.getInstance().scheduleReconnect();
        }
    }

    public void onError(Exception ex) {
        CFMCLogger.error("WebSocket 错误", ex);
    }

    /* =========================================================================
     * 服务端事件 (由包 handle() 回调)
     * ======================================================================= */

    public void onHandshakeAck(HandshakeAckPacket packet) {
        currentRegion = packet.regionX + "," + packet.regionZ;
        state = State.IN_GAME;
        CFMCLogger.info("握手完成, 区域 " + currentRegion + " 视距 " + packet.viewDistance);
    }

    public void onJoinGame(JoinGamePacket packet) {
        CFMCLogger.info("加入游戏: entityId=" + packet.entityId
                + " 出生点 (" + packet.spawnX + ", " + packet.spawnY + ", " + packet.spawnZ + ")");
        // TODO(Phase 2): 传送到出生点 + 显示 HUD
    }

    public void onChunkReceived(ChunkDataPacket packet) { /* 由 ChunkLoader 内部处理 */ }

    public void onServerDisconnect(String reason) {
        serverDisconnectNotified = true;
        CFMCLogger.warn("服务端断开: " + reason);
        disconnect();
        // [Phase 2] 断开展示移交 loader 层 (聊天提示 / CFMCDisconnectScreen)
        com.cfmc.common.platform.CFMCWorldBridge wb = com.cfmc.common.platform.CFMCWorldBridgeHolder.get();
        if (wb != null) wb.onServerDisconnected(reason);
    }

    public void onServerKeepAlive() {
        lastServerKeepAliveMs = System.currentTimeMillis();
    }
}

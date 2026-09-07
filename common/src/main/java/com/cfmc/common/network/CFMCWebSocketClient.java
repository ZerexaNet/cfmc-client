package com.cfmc.common.network;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * ============================================================================
 * WebSocket 底层客户端 — 基于 Java-WebSocket, 只管"管道", 不懂协议
 * ============================================================================
 * 职责分离:
 *   - 本类: 连接生命周期 + 原始字节收发 (带压缩标志的完整帧在此层不解析)
 *   - CFMCNetworkManager: 帧解析/包分派/状态机
 *
 * 与原版 TCP 的差异:
 *   原版 ClientConnection 是 Netty Channel; 本类是 WS Message 封包,
 *   一条 WS 消息可含多个协议帧 (Length 自描述, 由上层循环切片)。
 *
 * 线程模型: WS 回调在 IO 线程; 所有 Minecraft 世界操作必须经
 *   MinecraftClient.execute(...) 切回主线程 (在 NetworkManager 中处理)。
 */
public class CFMCWebSocketClient extends WebSocketClient {

    /** 上层处理器 (NetworkManager 单例) */
    private final CFMCNetworkManager manager;

    public CFMCWebSocketClient(URI uri, Map<String, String> headers, CFMCNetworkManager manager) {
        super(uri, headers);
        this.manager = manager;
        // 连接超时 10s (边缘节点全球接入, 正常 <1s)
        setConnectionLostTimeout(10);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        manager.onOpen(handshake.getHttpStatus());
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        // 二进制消息: 交给协议层 (一条消息可能包含多个帧)
        manager.onBinaryMessage(bytes.array());
    }

    @Override
    public void onMessage(String message) {
        // 文本消息: 服务端 JSON 调试通道 (welcome/echo)
        manager.onTextMessage(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        manager.onClose(code, reason, remote);
    }

    @Override
    public void onError(Exception ex) {
        manager.onError(ex);
    }
}

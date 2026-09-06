package com.cfmc.client.protocol.packets.clientbound;

import com.cfmc.client.util.CFMCConstants;
import com.cfmc.client.protocol.CFMCPacket;
import com.cfmc.client.protocol.PacketReader;
import com.cfmc.client.protocol.PacketWriter;

/**
 * ChatMessage (0x09, S→C) — 聊天消息 (v0.1: 纯文本, JSON组件 Phase 3)
 * payload: message(String)
 */
public class ChatMessagePacket extends CFMCPacket {
    public String message;

    public ChatMessagePacket() {
        super(CFMCConstants.CB_CHAT_MESSAGE);
    }

    @Override
    public void encode(PacketWriter writer) { /* S2C */ }

    @Override
    public void decode(PacketReader reader) {
        message = reader.readString(1024);
    }

    @Override
    public void handle() {
        // 在客户端聊天 HUD 显示 (Phase 2: MinecraftClient.inGameHud.getChatHud().addMessage)
        com.cfmc.client.util.CFMCLogger.info("[聊天] " + message);
    }
}

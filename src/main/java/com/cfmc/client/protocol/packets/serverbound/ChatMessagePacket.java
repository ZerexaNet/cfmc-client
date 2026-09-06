package com.cfmc.client.protocol.packets.serverbound;

import com.cfmc.client.util.CFMCConstants;
import com.cfmc.client.protocol.CFMCPacket;
import com.cfmc.client.protocol.PacketReader;
import com.cfmc.client.protocol.PacketWriter;

/**
 * ChatMessage (0x17, C→S) — 发送聊天
 * payload: message(String≤256)
 */
public class ChatMessagePacket extends CFMCPacket {
    public String message;

    public ChatMessagePacket() {
        super(CFMCConstants.SB_CHAT_MESSAGE);
    }

    public ChatMessagePacket(String message) {
        super(CFMCConstants.SB_CHAT_MESSAGE);
        this.message = message;
    }

    @Override
    public void encode(PacketWriter writer) {
        writer.writeString(message, 256);
    }

    @Override
    public void decode(PacketReader reader) { /* C2S */ }

    @Override
    public void handle() { /* C2S */ }
}

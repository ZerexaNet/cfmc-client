package com.cfmc.common.protocol.packets.clientbound;

import com.cfmc.common.platform.CFMCWorldBridge;
import com.cfmc.common.platform.CFMCWorldBridgeHolder;
import com.cfmc.common.util.CFMCConstants;
import com.cfmc.common.protocol.CFMCPacket;
import com.cfmc.common.protocol.PacketReader;
import com.cfmc.common.protocol.PacketWriter;

/**
 * PlayerInfo (0x08, S→C) — 玩家列表增删
 * payload: action(U8: 0=join 1=leave) | uuid(String) | name(String)
 */
public class PlayerInfoPacket extends CFMCPacket {
    public static final int ACTION_JOIN = 0;
    public static final int ACTION_LEAVE = 1;

    public int action;
    public String uuid;
    public String name;

    public PlayerInfoPacket() {
        super(CFMCConstants.CB_PLAYER_INFO);
    }

    @Override
    public void encode(PacketWriter writer) { /* S2C */ }

    @Override
    public void decode(PacketReader reader) {
        action = reader.readUnsignedByte();
        uuid = reader.readString(64);
        name = reader.readString(16);
    }

    @Override
    public void handle() {
        // [Phase 2] loader 层提示玩家进出 (Tab 列表/玩家实体渲染留待后续阶段)
        CFMCWorldBridge wb = CFMCWorldBridgeHolder.get();
        if (wb != null) wb.onPlayerInfo(action, name);
    }
}

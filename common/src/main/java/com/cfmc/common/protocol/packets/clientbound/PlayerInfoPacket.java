package com.cfmc.common.protocol.packets.clientbound;

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
        // TODO(Phase 2): 更新 Tab 列表 / 生成或销毁玩家实体
    }
}

package com.cfmc.client.protocol.packets.clientbound;

import com.cfmc.client.network.CFMCNetworkManager;
import com.cfmc.client.protocol.CFMCPacket;
import com.cfmc.client.protocol.PacketReader;
import com.cfmc.client.protocol.PacketWriter;
import com.cfmc.client.util.CFMCConstants;

/**
 * JoinGame (0x02, S→C) — 加入游戏
 * payload: entityId(I32) | gamemode(U8) | dimension(I32) | spawnX/Y/Z(Double×3)
 * 与服务端 RegionDO join 包编码一致。
 */
public class JoinGamePacket extends CFMCPacket {
    public int entityId;
    public int gamemode;
    public int dimension;
    public double spawnX, spawnY, spawnZ;

    public JoinGamePacket() {
        super(CFMCConstants.CB_JOIN_GAME);
    }

    @Override
    public void encode(PacketWriter writer) { /* S2C */ }

    @Override
    public void decode(PacketReader reader) {
        entityId = reader.readInt();
        gamemode = reader.readUnsignedByte();
        dimension = reader.readInt();
        spawnX = reader.readDouble();
        spawnY = reader.readDouble();
        spawnZ = reader.readDouble();
    }

    @Override
    public void handle() {
        // TODO(Phase 2): 触发进入世界流程 (设置玩家实体ID/出生点/游戏模式)
        CFMCNetworkManager.getInstance().onJoinGame(this);
    }
}

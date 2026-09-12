package com.cfmc.common.protocol.packets.clientbound;

import com.cfmc.common.network.CFMCNetworkManager;
import com.cfmc.common.platform.CFMCWorldBridge;
import com.cfmc.common.platform.CFMCWorldBridgeHolder;
import com.cfmc.common.protocol.CFMCPacket;
import com.cfmc.common.protocol.PacketReader;
import com.cfmc.common.protocol.PacketWriter;
import com.cfmc.common.util.CFMCConstants;

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
        CFMCNetworkManager.getInstance().onJoinGame(this);
        // [Phase 2] 灌入客户端世界: 传送到服务端恢复位置/出生点 (loader 层实现)
        CFMCWorldBridge wb = CFMCWorldBridgeHolder.get();
        if (wb != null) wb.onJoinGame(entityId, gamemode, spawnX, spawnY, spawnZ);
    }
}

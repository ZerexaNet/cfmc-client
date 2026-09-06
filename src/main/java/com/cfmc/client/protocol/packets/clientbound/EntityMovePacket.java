package com.cfmc.client.protocol.packets.clientbound;

import com.cfmc.client.util.CFMCConstants;
import com.cfmc.client.protocol.CFMCPacket;
import com.cfmc.client.protocol.PacketReader;
import com.cfmc.client.protocol.PacketWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * EntityMove (0x06, S→C) — 实体位置/朝向批量更新 (差量同步核心包)
 * payload: count(VarInt) + count × { uuid(String) x(D) y(D) z(D) yaw(F) pitch(F) }
 * 与服务端 RegionDO.#gameTick 的 movers 编码一致。
 */
public class EntityMovePacket extends CFMCPacket {
    public static class Entry {
        public String uuid;
        public double x, y, z;
        public float yaw, pitch;
    }

    public final List<Entry> entries = new ArrayList<>();

    public EntityMovePacket() {
        super(CFMCConstants.CB_ENTITY_MOVE);
    }

    @Override
    public void encode(PacketWriter writer) { /* S2C */ }

    @Override
    public void decode(PacketReader reader) {
        int count = reader.readVarInt();
        entries.clear();
        for (int i = 0; i < count; i++) {
            Entry e = new Entry();
            e.uuid = reader.readString(64);
            e.x = reader.readDouble();
            e.y = reader.readDouble();
            e.z = reader.readDouble();
            e.yaw = reader.readFloat();
            e.pitch = reader.readFloat();
            entries.add(e);
        }
    }

    @Override
    public void handle() {
        // TODO(Phase 2): 交给 CFMCInterpolationHelper 做位置插值 (120ms 缓动)
    }
}

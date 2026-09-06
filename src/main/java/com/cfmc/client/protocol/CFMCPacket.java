package com.cfmc.client.protocol;

/**
 * ============================================================================
 * 包基类 — 所有 CFMC 包的统一契约
 * ============================================================================
 * 生命周期:
 *   C2S: new → encode(PacketWriter) → NetworkManager.sendPacket
 *   S2C: registry.create(id) → decode(PacketReader) → handle(context)
 *
 * 字段式访问器 (public final 字段 + decode 赋值) 而非 getter:
 *   这些对象是一次性消息载体, 不是领域模型, 直接字段访问更直观。
 */
public abstract class CFMCPacket {
    protected int packetId;

    public CFMCPacket(int packetId) {
        this.packetId = packetId;
    }

    public int getPacketId() {
        return packetId;
    }

    /** C2S: 序列化 payload (不含帧头) */
    public abstract void encode(PacketWriter writer);

    /** S2C: 从 payload 反序列化 */
    public abstract void decode(PacketReader reader);

    /** 包处理入口 (在客户端主线程上执行) */
    public abstract void handle();
}

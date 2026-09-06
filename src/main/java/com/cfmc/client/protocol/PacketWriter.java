package com.cfmc.client.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * ============================================================================
 * 二进制写入器 (Big-Endian) — 对应服务端 packet-writer.js
 * ============================================================================
 * also 提供 frame() 帧封装: Length(VarInt) | PacketID(VarInt) | Flags(U8) | Payload
 */
public class PacketWriter {
    private ByteBuffer buf;

    public PacketWriter(int initialCapacity) {
        this.buf = ByteBuffer.allocate(initialCapacity);
    }

    private void ensure(int extra) {
        if (buf.remaining() >= extra) return;
        int needed = buf.position() + extra;
        int cap = buf.capacity();
        while (cap < needed) cap *= 2;
        ByteBuffer next = ByteBuffer.allocate(cap);
        buf.flip();
        next.put(buf);
        buf = next;
    }

    public PacketWriter writeBoolean(boolean b) {
        ensure(1);
        buf.put((byte) (b ? 1 : 0));
        return this;
    }

    public PacketWriter writeUnsignedByte(int v) {
        ensure(1);
        buf.put((byte) (v & 0xFF));
        return this;
    }

    public PacketWriter writeShort(int v) {
        ensure(2);
        buf.putShort((short) v);
        return this;
    }

    public PacketWriter writeInt(int v) {
        ensure(4);
        buf.putInt(v);
        return this;
    }

    public PacketWriter writeLong(long v) {
        ensure(8);
        buf.putLong(v);
        return this;
    }

    public PacketWriter writeFloat(float v) {
        ensure(4);
        buf.putFloat(v);
        return this;
    }

    public PacketWriter writeDouble(double v) {
        ensure(8);
        buf.putDouble(v);
        return this;
    }

    public PacketWriter writeString(String s) {
        return writeString(s, 32767);
    }

    public PacketWriter writeString(String s, int maxLength) {
        byte[] data = s.getBytes(StandardCharsets.UTF_8);
        ensure(5 + data.length);
        CFMCBufferUtils.writeVarInt(buf, data.length);
        buf.put(data);
        return this;
    }

    public PacketWriter writeVarInt(int v) {
        ensure(5);
        CFMCBufferUtils.writeVarInt(buf, v);
        return this;
    }

    public PacketWriter writeBytes(byte[] data) {
        ensure(data.length);
        buf.put(data);
        return this;
    }

    /** 已写内容 (拷贝) */
    public byte[] toArray() {
        byte[] out = new byte[buf.position()];
        System.arraycopy(buf.array(), 0, out, 0, out.length);
        return out;
    }

    /**
     * 帧封装 (与 PacketWriter.frame 服务端一致):
     * Length(VarInt, 不含自身) | PacketID(VarInt) | Flags(U8) | Payload
     */
    public static byte[] frame(int packetId, int flags, byte[] payload) {
        PacketWriter head = new PacketWriter(8);
        head.writeVarInt(packetId);
        head.writeUnsignedByte(flags);

        PacketWriter out = new PacketWriter(5 + head.buf.position() + payload.length);
        out.writeVarInt(head.buf.position() + payload.length);
        out.writeBytes(head.toArray());
        out.writeBytes(payload);
        return out.toArray();
    }
}

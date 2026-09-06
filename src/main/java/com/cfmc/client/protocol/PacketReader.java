package com.cfmc.client.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * ============================================================================
 * 二进制读取器 (Big-Endian) — 对应服务端 packet-reader.js
 * ============================================================================
 * 与服务端 PacketReader 的对齐点:
 *   - 整数/浮点统一大端序 (原版网络字节序)
 *   - VarInt/VarLong 与 CFMCBufferUtils 共享逻辑 (LEB128 小端位序)
 *   - 字符串 = VarInt 长度前缀 + UTF-8
 */
public class PacketReader {
    private final ByteBuffer buf;

    public PacketReader(byte[] bytes) {
        this(ByteBuffer.wrap(bytes));
    }

    public PacketReader(ByteBuffer buf) {
        this.buf = buf;
    }

    public int remaining() {
        return buf.remaining();
    }

    /* ---- VarInt / VarLong (与 CFMCBufferUtils 一致) ---- */

    public int readVarInt() {
        return CFMCBufferUtils.readVarInt(buf);
    }

    public long readVarLong() {
        return CFMCBufferUtils.readVarLong(buf);
    }

    /* ---- 标量 (大端) ---- */

    public boolean readBoolean() {
        return buf.get() != 0;
    }

    public int readUnsignedByte() {
        return buf.get() & 0xFF;
    }

    public int readShort() {
        return buf.getShort();
    }

    public int readUnsignedShort() {
        return buf.getShort() & 0xFFFF;
    }

    public int readInt() {
        return buf.getInt();
    }

    public long readLong() {
        return buf.getLong();
    }

    public float readFloat() {
        return buf.getFloat();
    }

    public double readDouble() {
        return buf.getDouble();
    }

    public String readString(int maxLength) {
        return CFMCBufferUtils.readString(buf, maxLength);
    }

    /** 定长字节数组 */
    public byte[] readBytes(int n) {
        byte[] out = new byte[n];
        buf.get(out);
        return out;
    }

    public ByteBuffer raw() {
        return buf;
    }
}

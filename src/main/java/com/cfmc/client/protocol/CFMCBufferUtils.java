package com.cfmc.client.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * ============================================================================
 * VarInt/VarLong 编解码工具 — 与服务端 packet-reader.js/packet-writer.js 逐字节一致!
 * ============================================================================
 * 规则 (wiki.vg / LEB128):
 *   - 每字节低 7 位承载数据, 最高位 1 = 后续还有字节
 *   - 位序: 第一个字节的 bit0 是整数的最低位 (小端位序)
 *   - VarInt 最多 5 字节, VarLong 最多 10 字节
 */
public final class CFMCBufferUtils {
    private CFMCBufferUtils() {}

    /** 写 VarInt (负数按 Java int 无符号处理, 与服务端 (value >>> 0) 行为一致) */
    public static void writeVarInt(ByteBuffer buf, int value) {
        while (true) {
            if ((value & ~0x7F) == 0) {
                buf.put((byte) value);
                return;
            }
            buf.put((byte) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
    }

    /** 读 VarInt (与服务端 readVarInt 一致: 32位有符号回卷) */
    public static int readVarInt(ByteBuffer buf) {
        int result = 0;
        int shift = 0;
        while (true) {
            if (shift >= 32) throw new RuntimeException("VarInt too big");
            byte b = buf.get();
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return result;
            shift += 7;
        }
    }

    /** 写 VarLong */
    public static void writeVarLong(ByteBuffer buf, long value) {
        while (true) {
            if ((value & ~0x7FL) == 0) {
                buf.put((byte) value);
                return;
            }
            buf.put((byte) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
    }

    /** 读 VarLong */
    public static long readVarLong(ByteBuffer buf) {
        long result = 0;
        int shift = 0;
        while (true) {
            if (shift >= 64) throw new RuntimeException("VarLong too big");
            byte b = buf.get();
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return result;
            shift += 7;
        }
    }

    /** VarInt 长度前缀字符串 (与服务端 writeString/readString 对称) */
    public static void writeString(ByteBuffer buf, String s, int maxLength) {
        byte[] data = s.getBytes(StandardCharsets.UTF_8);
        if (data.length > maxLength) throw new RuntimeException("String too long: " + data.length);
        writeVarInt(buf, data.length);
        buf.put(data);
    }

    public static String readString(ByteBuffer buf, int maxLength) {
        int len = readVarInt(buf);
        if (len < 0 || len > maxLength) throw new RuntimeException("String length invalid: " + len);
        byte[] data = new byte[len];
        buf.get(data);
        return new String(data, StandardCharsets.UTF_8);
    }

    /** 计算 VarInt 编码后的字节数 (预分配缓冲用) */
    public static int varIntLength(int value) {
        int n = 1;
        while ((value & ~0x7F) != 0) {
            value >>>= 7;
            n++;
        }
        return n;
    }
}

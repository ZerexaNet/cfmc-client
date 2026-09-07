package com.cfmc.common.network;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * ============================================================================
 * 客户端压缩工具 — deflate-raw 解压 (与服务端 compression.js 对齐)
 * ============================================================================
 * 服务端: CompressionStream('deflate-raw')
 * Java端: Inflater(nowrap=true) ← nowrap 即 raw deflate, 逐字节兼容!
 *
 * TODO(Phase 2): Deflater 压缩方向 (客户端大包如 ChunkData 请求)
 */
public final class CFMCCompression {
    private CFMCCompression() {}

    /** 解压 deflate-raw 数据 */
    public static byte[] inflateRaw(byte[] data) throws DataFormatException {
        Inflater inflater = new Inflater(true); // true = nowrap (raw deflate)
        try {
            inflater.setInput(data);
            // 区块数据理论可达 ~16KB (4096索引×16bit), 给足缓冲
            byte[] out = new byte[Math.max(4096, data.length * 8)];
            int n = inflater.inflate(out);
            return java.util.Arrays.copyOf(out, n);
        } finally {
            inflater.end();
        }
    }
}

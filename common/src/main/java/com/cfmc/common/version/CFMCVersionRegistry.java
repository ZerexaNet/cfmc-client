package com.cfmc.common.version;

import com.cfmc.common.platform.CFMCPlatformHolder;

/**
 * ============================================================================
 * MC 版本注册表 (客户端侧) — 与服务端 version-registry.js 逐字镜像!
 * ============================================================================
 *
 * 对齐文件: cfmc-server/src/protocol/version-registry.js
 * 数据源: wiki.vg "Protocol version numbers" (截至 1.21.8, 2025-08)
 *
 * 用途:
 *   1. 握手时向服务端上报本客户端的 MC 协议号 (服务端据此选适配器)
 *   2. 解析服务端 ACK 里的支持范围, 提示不兼容 (而不是黑屏无解释)
 *
 * 加新 MC 版本 = 在 MC_PROTOCOLS 加一行 (两端同步)。
 */
public final class CFMCVersionRegistry {
    private CFMCVersionRegistry() {}

    /** 一条 MC 版本记录 */
    public record McVersion(int protocol, String version) {}

    /**
     * MC 版本 → 协议号表 (升序; 与服务端 MC_PROTOCOLS 一致)
     * 注意: 同一协议号可能覆盖多个版本 (如 767 = 1.21 / 1.21.1),
     *       这里登记该协议号的代表版本, 精确版本串仅在日志中使用。
     */
    private static final McVersion[] MC_PROTOCOLS = {
        new McVersion(47,  "1.8"),
        new McVersion(107, "1.9"),
        new McVersion(108, "1.9.1"),
        new McVersion(110, "1.9.4"),
        new McVersion(210, "1.11"),
        new McVersion(316, "1.11.2"),
        new McVersion(340, "1.12.2"),
        new McVersion(393, "1.13"),
        new McVersion(404, "1.13.2"),
        new McVersion(480, "1.14"),
        new McVersion(498, "1.14.4"),
        new McVersion(575, "1.15"),
        new McVersion(578, "1.15.2"),
        new McVersion(735, "1.16"),
        new McVersion(751, "1.16.1"),
        new McVersion(754, "1.16.5"),
        new McVersion(755, "1.17"),
        new McVersion(756, "1.17.1"),
        new McVersion(757, "1.18"),
        new McVersion(758, "1.18.2"),
        new McVersion(759, "1.19"),
        new McVersion(760, "1.19.1"),
        new McVersion(761, "1.19.3"),
        new McVersion(762, "1.19.4"),
        new McVersion(763, "1.20.1"),
        new McVersion(764, "1.20.2"),
        new McVersion(765, "1.20.4"),
        new McVersion(766, "1.20.6"),
        new McVersion(767, "1.21"),
        new McVersion(768, "1.21.3"),
        new McVersion(769, "1.21.4"),
        new McVersion(770, "1.21.5"),
        new McVersion(771, "1.21.6"),
        new McVersion(772, "1.21.8"),
    };

    /** 支持范围 (与服务器一致) */
    public static final int OLDEST_PROTOCOL = MC_PROTOCOLS[0].protocol();
    public static final int LATEST_PROTOCOL = MC_PROTOCOLS[MC_PROTOCOLS.length - 1].protocol();

    /** 精确版本串 → 协议号 (处理同一协议号的别名版本) */
    private static final java.util.Map<String, Integer> VERSION_TO_PROTO = new java.util.HashMap<>();
    static {
        for (McVersion v : MC_PROTOCOLS) VERSION_TO_PROTO.put(v.version(), v.protocol());
        // 别名版本 (与服务端 aliases 对齐)
        VERSION_TO_PROTO.put("1.8.9", 47);   VERSION_TO_PROTO.put("1.10.2", 110);
        VERSION_TO_PROTO.put("1.12", 316);   VERSION_TO_PROTO.put("1.12.1", 316);
        VERSION_TO_PROTO.put("1.13.1", 393); VERSION_TO_PROTO.put("1.14.3", 480);
        VERSION_TO_PROTO.put("1.15.1", 575); VERSION_TO_PROTO.put("1.16.4", 754);
        VERSION_TO_PROTO.put("1.18.1", 757); VERSION_TO_PROTO.put("1.19.2", 760);
        VERSION_TO_PROTO.put("1.20.3", 765); VERSION_TO_PROTO.put("1.20.5", 766);
        VERSION_TO_PROTO.put("1.21.1", 767); VERSION_TO_PROTO.put("1.21.2", 768);
        VERSION_TO_PROTO.put("1.21.7", 772);
    }

    /**
     * 当前运行环境的 MC 协议号 (握手上报用)
     * 优先按 loader 上报的版本串精确查找; 查不到按字符串前缀猜;
     * 仍失败返回 -1 (服务端会走 generic 兜底, 不拒连)。
     */
    public static int currentProtocol() {
        String v = CFMCPlatformHolder.get().mcVersion();
        Integer proto = VERSION_TO_PROTO.get(v);
        if (proto != null) return proto;
        // 前缀匹配: "1.21.4-pre1" → "1.21.4"
        for (McVersion mv : MC_PROTOCOLS) {
            if (v.startsWith(mv.version())) return mv.protocol();
        }
        return -1;
    }

    /** 版本串 → 协议号 (未知返回 -1) */
    public static int protocolOf(String mcVersion) {
        return VERSION_TO_PROTO.getOrDefault(mcVersion, -1);
    }

    /** 协议号是否在本客户端认知范围内 (ACK 范围校验) */
    public static boolean inSupportedRange(int proto) {
        return proto >= OLDEST_PROTOCOL && proto <= LATEST_PROTOCOL;
    }
}

package com.cfmc.common.platform;

/**
 * 世界桥接持有器 — 服务定位器模式 (loader 入口注入, common 侧只读)
 * 与 {@link CFMCPlatformHolder} 对称; 差异: 世界桥不提供 Dev 兜底
 * (无 MC 环境时"写世界"本身无意义), 未注入时 get() 返回 null,
 * 调用方 (各包 handle()) 判空跳过 —— 保证网络层可独立运行/测试。
 */
public final class CFMCWorldBridgeHolder {
    private CFMCWorldBridgeHolder() {}

    private static volatile CFMCWorldBridge bridge;

    /** loader 入口调用 (幂等: 首次注入生效, 重复注入以首次为准防覆盖) */
    public static void set(CFMCWorldBridge b) {
        if (bridge == null) {
            bridge = b;
        }
    }

    /** 未注册时返回 null (网络层独立运行场景) */
    public static CFMCWorldBridge get() {
        return bridge;
    }
}

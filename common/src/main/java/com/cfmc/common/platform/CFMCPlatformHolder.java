package com.cfmc.common.platform;

import com.cfmc.common.util.CFMCLogger;

/**
 * 平台实现持有器 — 服务定位器模式 (loader 侧在入口注入, common 侧只读)
 *
 * 时序: loader 入口 (ModInitializer 构造 / @Mod 构造) 第一行调用
 *       {@link #set(CFMCPlatform)} → 之后 common 全链路可用。
 *       未注入时返回 DevPlatform (纯 JVM 调试用: 无 MC 版本/配置走当前目录),
 *       保证单元测试与独立调试无需加载器也能跑。
 */
public final class CFMCPlatformHolder {
    private CFMCPlatformHolder() {}

    private static volatile CFMCPlatform platform;

    /** loader 入口调用 (幂等: 首次注入生效, 重复注入以首次为准防覆盖) */
    public static void set(CFMCPlatform p) {
        if (platform == null) {
            platform = p;
            CFMCLogger.info("CFMC 平台已注入: " + p.loaderName() + " @ MC " + p.mcVersion());
        }
    }

    /** 获取平台 (永不返回 null — 兜底 DevPlatform) */
    public static CFMCPlatform get() {
        if (platform == null) {
            platform = new DevPlatform();
        }
        return platform;
    }

    /** 纯 JVM 环境 (单测/无加载器) 的兜底实现 */
    private static final class DevPlatform implements CFMCPlatform {
        @Override public String mcVersion() { return "unknown"; }
        @Override public String loaderName() { return "dev"; }
        @Override public java.nio.file.Path configDir() { return java.nio.file.Paths.get("."); }
    }
}

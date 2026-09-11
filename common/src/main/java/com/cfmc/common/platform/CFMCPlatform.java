package com.cfmc.common.platform;

import java.nio.file.Path;

/**
 * ============================================================================
 * 平台抽象 — common 模块与 Minecraft 加载器 (Fabric/NeoForge) 的唯一边界
 * ============================================================================
 *
 * 为什么需要它 (全版本支持架构核心):
 *   common 模块持有网络/协议/认证/区块解码等 90% 逻辑, 且【零 Minecraft 依赖】
 *   —— 这使它能被一份字节码跑在 1.8~1.21.x 所有版本上。
 *   唯一的版本差异 (MC 版本号 / 配置目录 / 加载器名) 由加载器侧实现本接口,
 *   在 Mod 入口最早期 (构造/preLaunch) 注入给 CFMCPlatformHolder。
 *
 * 铁律: common 里任何类不得直接 import net.minecraft.* / net.fabricmc.* /
 *   net.neoforged.* —— 需要平台能力时只能经本接口 (CI 会用 ArchUnit 检查, TODO)。
 */
public interface CFMCPlatform {
    /** 当前 Minecraft 版本串 (如 "1.20.4"), 来自加载器运行时信息 */
    String mcVersion();

    /** 加载器名 ("fabric" / "neoforge"), 用于日志与服务端上报 */
    String loaderName();

    /** 配置目录 (loader 已按 MC 规范初始化, 如 .minecraft/config) */
    Path configDir();
}

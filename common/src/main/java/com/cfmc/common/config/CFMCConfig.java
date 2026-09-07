package com.cfmc.common.config;

import com.cfmc.common.util.CFMCLogger;
import com.cfmc.common.platform.CFMCPlatformHolder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * ============================================================================
 * Mod 配置 — java.util.Properties 而非 JSON (设计决策)
 * ============================================================================
 * 为什么不用 Gson+JSON: 配置只有 7 个标量, Properties 足够且 JDK 自带
 * 零依赖; 用户手改也直观。Phase 3 GUI 配置界面再考虑迁移。
 *
 * 全版本改造: 不再直接依赖 FabricLoader —— 配置目录由
 * CFMCPlatformHolder (loader 侧注入) 提供, common 模块保持零 MC 依赖。
 */
public final class CFMCConfig {
    private static final CFMCConfig INSTANCE = new CFMCConfig();

    public static CFMCConfig get() {
        return INSTANCE;
    }

    private CFMCConfig() {}

    // ---- 可配置项 (默认值与 fabric.mod.json custom.cfmc:config 一致) ----
    public String defaultServerAddress = "ws://localhost:8787";
    public String authMode = "hybrid";           // online / offline / skin_server / hybrid
    public String skinServerUrl = "https://ely.by";
    public boolean enableDebugOverlay = true;
    public boolean autoReconnect = true;
    public long reconnectDelayMs = 5000;
    public int maxReconnectAttempts = 3;

    private static final String FILE_NAME = "cfmc-client.properties";

    /** 启动时加载 (文件不存在则用默认值) */
    public void load() {
        Path path = CFMCPlatformHolder.get().configDir().resolve(FILE_NAME);
        if (!Files.exists(path)) {
            save(); // 首次运行生成默认配置, 方便用户手改
            return;
        }
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            p.load(in);
            defaultServerAddress = p.getProperty("serverAddress", defaultServerAddress);
            authMode = p.getProperty("authMode", authMode);
            skinServerUrl = p.getProperty("skinServerUrl", skinServerUrl);
            enableDebugOverlay = Boolean.parseBoolean(p.getProperty("debugOverlay", "true"));
            autoReconnect = Boolean.parseBoolean(p.getProperty("autoReconnect", "true"));
            reconnectDelayMs = Long.parseLong(p.getProperty("reconnectDelayMs", "5000"));
            maxReconnectAttempts = Integer.parseInt(p.getProperty("maxReconnectAttempts", "3"));
            CFMCLogger.info("配置已加载: " + path);
        } catch (Exception e) {
            CFMCLogger.error("配置加载失败, 使用默认值", e);
        }
    }

    /** 保存到磁盘 */
    public void save() {
        Path path = CFMCPlatformHolder.get().configDir().resolve(FILE_NAME);
        Properties p = new Properties();
        p.setProperty("serverAddress", defaultServerAddress);
        p.setProperty("authMode", authMode);
        p.setProperty("skinServerUrl", skinServerUrl);
        p.setProperty("debugOverlay", String.valueOf(enableDebugOverlay));
        p.setProperty("autoReconnect", String.valueOf(autoReconnect));
        p.setProperty("reconnectDelayMs", String.valueOf(reconnectDelayMs));
        p.setProperty("maxReconnectAttempts", String.valueOf(maxReconnectAttempts));
        try (OutputStream out = Files.newOutputStream(path)) {
            p.store(out, "CFMC Client Config (手动修改后重启生效)");
        } catch (IOException e) {
            CFMCLogger.error("配置保存失败", e);
        }
    }
}

package com.cfmc.common.network;

import com.cfmc.common.config.CFMCConfig;
import com.cfmc.common.util.CFMCLogger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ============================================================================
 * 自动重连 — 指数退避 (5s → 10s → 20s, 上限 maxReconnectAttempts 次)
 * ============================================================================
 * cfmc.md 要求 #8: 网络不稳定是常态, 要优雅降级。
 * TODO(Phase 3): 断线重连需恢复断开前的状态 (RegionDO 持久化了玩家位置,
 *   服务端已就绪; 客户端重连后重新走握手→JoinGame 流程即可)。
 */
public final class CFMCReconnectHandler {
    private static final CFMCReconnectHandler INSTANCE = new CFMCReconnectHandler();

    public static CFMCReconnectHandler getInstance() {
        return INSTANCE;
    }

    private CFMCReconnectHandler() {}

    private final AtomicInteger attempts = new AtomicInteger(0);
    private ScheduledExecutorService scheduler;
    private volatile String lastServerUrl;

    /** 记录最近一次成功连接的地址 (重连目标) */
    public void remember(String serverUrl) {
        lastServerUrl = serverUrl;
    }

    /** 调度一次重连尝试 */
    public synchronized void scheduleReconnect() {
        if (lastServerUrl == null) return;
        int attempt = attempts.incrementAndGet();
        int max = CFMCConfig.get().maxReconnectAttempts;
        if (attempt > max) {
            CFMCLogger.warn("重连放弃 (已尝试 " + max + " 次)");
            attempts.set(0);
            return;
        }

        long delayMs = CFMCConfig.get().reconnectDelayMs * (1L << (attempt - 1)); // 5s,10s,20s...
        CFMCLogger.info("第 " + attempt + "/" + max + " 次重连将在 " + delayMs + "ms 后");

        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "CFMC-Reconnect");
                t.setDaemon(true);
                return t;
            });
        }
        scheduler.schedule(() -> {
            CFMCNetworkManager.getInstance().connect(lastServerUrl, null); // TODO: 复用缓存AuthResult
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    /** 连接成功后重置计数 (由 onHandshakeAck 调用) */
    public void onConnected() {
        attempts.set(0);
    }
}

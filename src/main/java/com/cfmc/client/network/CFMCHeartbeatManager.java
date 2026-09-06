package com.cfmc.client.network;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ============================================================================
 * 心跳管理器 — 每 10 秒发送 SB_KEEP_ALIVE (与服务端 200tick 周期对齐)
 * ============================================================================
 * 服务端 30 秒未收到心跳会踢人; 我们以 10 秒周期发送, 留 2 次容错。
 * 独立调度线程 (不走 Minecraft tick): 即使客户端卡顿也能保活。
 */
public final class CFMCHeartbeatManager {
    private static final CFMCHeartbeatManager INSTANCE = new CFMCHeartbeatManager();

    public static CFMCHeartbeatManager getInstance() {
        return INSTANCE;
    }

    private CFMCHeartbeatManager() {}

    private static final long INTERVAL_SECONDS = 10;

    private ScheduledExecutorService scheduler;

    public synchronized void start() {
        if (scheduler != null && !scheduler.isShutdown()) return; // 已在运行
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "CFMC-Heartbeat");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            try {
                CFMCNetworkManager.getInstance()
                        .sendPacket(new com.cfmc.client.protocol.packets.serverbound.KeepAlivePacket());
            } catch (Exception ignored) {
                // 断线时发送失败是正常的, close 回调负责清理
            }
        }, INTERVAL_SECONDS, INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }
}

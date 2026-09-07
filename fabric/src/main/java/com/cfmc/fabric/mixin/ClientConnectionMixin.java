package com.cfmc.fabric.mixin;

import com.cfmc.common.network.CFMCNetworkManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;

/**
 * ============================================================================
 * Mixin: 拦截网络连接 (cfmc.md 客户端关键技术点)
 * ============================================================================
 * 当连接目标是 CFMC 服务器时, 取消原生 TCP 通道, 改走 WebSocket。
 * 连接普通服务器时完全不受影响 (原版流程原样执行)。
 *
 * 识别 CFMC 服务器: CFMCConfig.defaultServerAddress 的 host:port 与
 * 目标地址匹配 (TODO: 服务器列表条目自定义标记)。
 *
 * ⚠️ yarn 映射名随版本变化: 1.20.4 中 ClientConnection.connect 的
 * 精确签名以运行时映射为准, 报错时请比对 jar 内映射调整 method 描述符。
 */
@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {

    @Inject(method = "connect(Ljava/net/InetSocketAddress;Z)Lnet/minecraft/network/ClientConnection;",
            at = @At("HEAD"), cancellable = true)
    private static void onConnect(InetSocketAddress address, boolean useEpoll, CallbackInfo ci) {
        if (CFMCNetworkManager.getInstance().isActive()) {
            ci.cancel(); // 已处于 CFMC 会话中, 不再走原版连接
        }
        // v0.1 策略: CFMC 连接统一由 CFMCClientMod 的登录界面发起 (主动式),
        // 不在原版 connect 里静默劫持 — 避免误伤正常服务器。
        // TODO(Phase 2): 目标地址匹配 CFMC 服务器时才 cancel 并转 WebSocket。
    }
}

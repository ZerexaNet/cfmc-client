package com.cfmc.neoforge.mixin;

import com.cfmc.common.network.CFMCNetworkManager;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Connection Mixin (Mojang 映射: net.minecraft.network.Connection)
 *
 * CFMC 架构: Mod 自建 WebSocket 通道 (CFMCWebSocketClient) 承载游戏包,
 * 原生 TCP Connection 保持原样负责登录/资源包等基础设施。
 * 本 Mixin 目前仅做连接生命周期观测 (Phase 2 将拦截 connect 重路由到 WS)。
 *
 * yarn 版对应类: net.minecraft.network.ClientConnection (见 fabric/ClientConnectionMixin)
 */
@Mixin(Connection.class)
public class ConnectionMixin {

    @Inject(method = "disconnect", at = @At("HEAD"))
    private void cfmc$onDisconnect(CallbackInfo ci) {
        // 原生连接断开时同步清理 CFMC 通道状态 (若处于连接中)
        var nm = CFMCNetworkManager.getInstance();
        if (nm.getState() != CFMCNetworkManager.State.DISCONNECTED
                && nm.getState() != CFMCNetworkManager.State.IN_GAME) {
            nm.disconnect();
        }
    }
}

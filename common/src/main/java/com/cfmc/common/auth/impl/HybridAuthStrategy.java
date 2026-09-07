package com.cfmc.common.auth.impl;

import com.cfmc.common.auth.AuthResult;
import com.cfmc.common.auth.CFMCAuthService;
import com.cfmc.common.util.CFMCLogger;

/**
 * ============================================================================
 * 混合认证 — 降级链 (与服务端 loginHybrid 策略镜像对称)
 * ============================================================================
 * 客户端侧策略:
 *   1. 有正版会话 → OnlineAuthStrategy
 *   2. 有密码    → SkinServerAuthStrategy
 *   3. 兜底      → OfflineAuthStrategy (保证"总能进服")
 */
public class HybridAuthStrategy implements CFMCAuthService {

    private final OnlineAuthStrategy online;
    private final SkinServerAuthStrategy skinServer;
    private final OfflineAuthStrategy offline = new OfflineAuthStrategy();

    public HybridAuthStrategy(OnlineAuthStrategy online, SkinServerAuthStrategy skinServer) {
        this.online = online;
        this.skinServer = skinServer;
    }

    @Override
    public AuthResult authenticate(String username, String password) throws AuthException {
        // 1. 正版优先
        if (online != null) {
            try {
                return online.authenticate(username, password);
            } catch (AuthException e) {
                CFMCLogger.warn("正版认证失败, 降级: " + e.getMessage());
            }
        }

        // 2. 皮肤站
        if (password != null && !password.isBlank()) {
            try {
                return skinServer.authenticate(username, password);
            } catch (AuthException e) {
                CFMCLogger.warn("皮肤站认证失败, 降级: " + e.getMessage());
            }
        }

        // 3. 离线兜底
        return offline.authenticate(username, password);
    }
}

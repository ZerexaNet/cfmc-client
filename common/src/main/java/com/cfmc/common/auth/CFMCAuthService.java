package com.cfmc.common.auth;

/**
 * 认证策略接口 — 四种模式的统一抽象
 *
 * 实现:
 *   OnlineAuthStrategy      正版 (Mojang session)
 *   OfflineAuthStrategy     离线 (本地 UUID v3)
 *   SkinServerAuthStrategy  皮肤站 (Yggdrasil authenticate)
 *   HybridAuthStrategy      混合降级链
 *
 * 服务端对应: src/workers/auth.js handleLogin 的四个分支。
 * ⚠️ 两端 UUID 算法必须一致 (offline 模式), 否则同一玩家在两端身份不同!
 *
 * 重要: authenticate 现在接受 serverUrl 参数, 所有策略通过服务端 /auth/login
 * 端点获取 JWT token, 不再本地计算身份。
 */
public interface CFMCAuthService {

    /**
     * 执行认证 (通过服务端 /auth/login 端点)
     * @param username 玩家输入的用户名
     * @param password 皮肤站密码 (offline 模式忽略; 可为 null)
     * @param serverUrl 服务器地址 (ws:// 或 wss://, 用于调用 /auth/login)
     * @return 认证结果 (含服务端签发的 JWT accessToken); 失败时抛 AuthException
     */
    AuthResult authenticate(String username, String password, String serverUrl) throws AuthException;

    /** 认证失败异常 (message 面向玩家展示, 需可读) */
    class AuthException extends Exception {
        public AuthException(String message) {
            super(message);
        }

        public AuthException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
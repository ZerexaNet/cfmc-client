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
 */
public interface CFMCAuthService {

    /**
     * 执行认证
     * @param username 玩家输入的用户名
     * @param password 皮肤站密码 (offline 模式忽略; 可为 null)
     * @return 认证结果; 失败时抛 AuthException
     */
    AuthResult authenticate(String username, String password) throws AuthException;

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

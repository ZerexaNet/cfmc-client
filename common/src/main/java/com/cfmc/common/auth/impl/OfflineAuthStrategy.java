package com.cfmc.common.auth.impl;

import com.cfmc.common.auth.AuthResult;
import com.cfmc.common.auth.CFMCAuthService;
import com.cfmc.common.util.CFMCLogger;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * ============================================================================
 * 离线认证 — 用户名即身份, UUID = nameUUIDFromBytes("OfflinePlayer:"+name)
 * ============================================================================
 * Java 端直接用 JDK 的 UUID.nameUUIDFromBytes (MD5 + v3 + RFC4122 variant),
 * 与服务端 offline-uuid.js (纯JS MD5) 已交叉验证一致:
 *   Notch → b50ad385-829d-3141-a216-7e7d7539ba7f
 *
 * ⚠️ 大小写敏感! "Steve" 与 "steve" 是两个不同身份 (与 Java 一致)。
 */
public class OfflineAuthStrategy implements CFMCAuthService {

    @Override
    public AuthResult authenticate(String username, String password) {
        if (username == null || username.isBlank() || username.length() > 16) {
            throw new IllegalArgumentException("用户名必须为 1-16 个字符");
        }
        // JDK 原生算法 — 与服务端 JS 实现逐字节一致 (服务端已用相同输入验证)
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
        CFMCLogger.info("离线认证: " + username + " → " + uuid);
        // 离线模式无服务端 JWT → accessToken 传 null, 走服务端匿名握手分支
        return new AuthResult(uuid.toString().replace("-", ""), username, null, "offline");
    }
}

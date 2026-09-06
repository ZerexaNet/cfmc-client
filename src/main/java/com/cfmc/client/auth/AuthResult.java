package com.cfmc.client.auth;

/**
 * 认证结果载体 — 与服务端 /auth/login 响应的 profile 字段对齐
 */
public class AuthResult {
    /** 玩家 UUID (无横杠小写, 与服务端 users 表一致) */
    public final String uuid;
    /** 玩家名 (认证后以服务端确认名为准) */
    public final String name;
    /** JWT accessToken (WS 握手 Authorization: Bearer 携带); null = 匿名 */
    public final String accessToken;
    /** 使用的认证模式: online / offline / skin_server */
    public final String mode;

    public AuthResult(String uuid, String name, String accessToken, String mode) {
        this.uuid = uuid;
        this.name = name;
        this.accessToken = accessToken;
        this.mode = mode;
    }

    /** 匿名结果 (开发调试) */
    public static AuthResult anonymous(String name) {
        return new AuthResult(null, name, null, "anonymous");
    }

    @Override
    public String toString() {
        return "AuthResult{name=" + name + ", mode=" + mode + "}";
    }
}
